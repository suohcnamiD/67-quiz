package dev.six_seven_quiz.quiz.service;

import dev.six_seven_quiz.user.profile.exception.InvalidImageException;
import dev.six_seven_quiz.user.profile.service.UploadsProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

/**
 * Stores cover / question / option images on disk under uploads/, mirroring
 * AvatarStorageService. All inputs are decoded via ImageIO (the Twelvemonkeys
 * plugin handles WebP), scaled down so the longer side fits the per-kind cap,
 * and re-encoded as JPEG. Files are written via atomic rename so a crash
 * mid-write doesn't leave a corrupt image at the target path.
 */
@Service
public class QuizImageStorageService {

    /**
     * Per-kind max longest side in pixels. Covers can carry a hero shot so
     * they get more headroom; per-question/per-option are inline elements.
     */
    public enum Kind {
        COVER("quizzes", 1280),
        QUESTION("questions", 640),
        OPTION("options", 480);

        final String subdir;
        final int maxSide;

        Kind(String subdir, int maxSide) {
            this.subdir = subdir;
            this.maxSide = maxSide;
        }
    }

    public static final long MAX_UPLOAD_BYTES = 4L * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/png", "image/jpeg", "image/jpg", "image/webp"
    );

    private final UploadsProperties uploadsProperties;

    public QuizImageStorageService(UploadsProperties uploadsProperties) {
        this.uploadsProperties = uploadsProperties;
    }

    private Path dirFor(Kind kind) throws IOException {
        Path dir = Paths.get(uploadsProperties.getDir(), kind.subdir);
        Files.createDirectories(dir);
        return dir;
    }

    private Path fileFor(Kind kind, UUID id) throws IOException {
        return dirFor(kind).resolve(id + ".jpg");
    }

    /**
     * Validate, decode, scale, write. Returns the relative path stored on the
     * entity (e.g. {@code "quizzes/<id>.jpg"}).
     */
    public String store(Kind kind, UUID id, MultipartFile file) {
        if (file.isEmpty()) {
            throw new InvalidImageException("Image upload was empty");
        }
        if (file.getSize() > MAX_UPLOAD_BYTES) {
            throw new dev.six_seven_quiz.user.profile.exception.AvatarTooLargeException(MAX_UPLOAD_BYTES);
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new InvalidImageException("Unsupported content type: " + contentType);
        }

        BufferedImage source;
        try (InputStream in = file.getInputStream()) {
            source = ImageIO.read(in);
        } catch (IOException e) {
            throw new InvalidImageException("Could not read uploaded image");
        }
        if (source == null) {
            throw new InvalidImageException("Uploaded bytes are not a recognisable image");
        }

        BufferedImage scaled = scaleDownToFit(source, kind.maxSide);

        try {
            Path target = fileFor(kind, id);
            Path tmp = target.resolveSibling(target.getFileName() + "." + UUID.randomUUID() + ".tmp");
            if (!ImageIO.write(scaled, "jpg", tmp.toFile())) {
                throw new InvalidImageException("Could not encode JPEG output");
            }
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            return kind.subdir + "/" + target.getFileName();
        } catch (IOException e) {
            throw new InvalidImageException("Could not write image file: " + e.getMessage());
        }
    }

    public byte[] read(Kind kind, UUID id) {
        try {
            return Files.readAllBytes(fileFor(kind, id));
        } catch (IOException e) {
            throw new InvalidImageException("Image not on disk");
        }
    }

    public void delete(Kind kind, UUID id) {
        try {
            Files.deleteIfExists(fileFor(kind, id));
        } catch (IOException e) {
            // Best-effort; the row is the source of truth.
        }
    }

    private BufferedImage scaleDownToFit(BufferedImage source, int maxSide) {
        int w = source.getWidth();
        int h = source.getHeight();
        if (w <= maxSide && h <= maxSide) {
            // Already small enough — still re-encode as JPEG to strip metadata
            // and normalise to TYPE_INT_RGB.
            BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = out.createGraphics();
            g.drawImage(source, 0, 0, null);
            g.dispose();
            return out;
        }
        double ratio = (double) maxSide / Math.max(w, h);
        int newW = Math.max(1, (int) Math.round(w * ratio));
        int newH = Math.max(1, (int) Math.round(h * ratio));
        Image hint = source.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
        BufferedImage out = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(hint, 0, 0, null);
        g.dispose();
        return out;
    }
}
