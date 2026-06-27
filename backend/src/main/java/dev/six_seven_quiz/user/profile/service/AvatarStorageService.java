package dev.six_seven_quiz.user.profile.service;

import dev.six_seven_quiz.user.ApplicationUser;
import dev.six_seven_quiz.user.profile.exception.AvatarNotFoundException;
import dev.six_seven_quiz.user.profile.exception.AvatarTooLargeException;
import dev.six_seven_quiz.user.profile.exception.InvalidImageException;
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

@Service
public class AvatarStorageService {

    static final int OUTPUT_SIZE_PX = 256;
    static final long MAX_UPLOAD_BYTES = 2L * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/png", "image/jpeg", "image/jpg", "image/webp"
    );

    private final UploadsProperties uploadsProperties;

    public AvatarStorageService(UploadsProperties uploadsProperties) {
        this.uploadsProperties = uploadsProperties;
    }

    private Path avatarsDir() throws IOException {
        Path dir = Paths.get(uploadsProperties.getDir(), "avatars");
        Files.createDirectories(dir);
        return dir;
    }

    private Path avatarFileFor(ApplicationUser user) throws IOException {
        return avatarsDir().resolve(user.getId() + ".jpg");
    }

    /**
     * Decode, crop to a centred square, scale to 256x256, and re-encode as JPEG
     * under uploads/avatars/<userId>.jpg. Returns the relative path stored on
     * the entity.
     */
    public String store(ApplicationUser user, MultipartFile file) {
        if (file.isEmpty()) {
            throw new InvalidImageException("Avatar upload was empty");
        }
        if (file.getSize() > MAX_UPLOAD_BYTES) {
            throw new AvatarTooLargeException(MAX_UPLOAD_BYTES);
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

        BufferedImage square = cropToCentredSquare(source);
        BufferedImage scaled = scaleTo(square, OUTPUT_SIZE_PX);

        try {
            Path target = avatarFileFor(user);
            // Write to a tmp file first then atomic-rename so a crash mid-write
            // doesn't leave a corrupt avatar in place.
            Path tmp = target.resolveSibling(target.getFileName() + "." + UUID.randomUUID() + ".tmp");
            if (!ImageIO.write(scaled, "jpg", tmp.toFile())) {
                throw new InvalidImageException("Could not encode JPEG output");
            }
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            // Path stored on the entity is relative to uploads dir so the dir can move freely.
            return "avatars/" + target.getFileName();
        } catch (IOException e) {
            throw new InvalidImageException("Could not write avatar file: " + e.getMessage());
        }
    }

    public byte[] read(ApplicationUser user) {
        if (user.getAvatarPath() == null) {
            throw new AvatarNotFoundException(user.getUsername());
        }
        try {
            return Files.readAllBytes(avatarFileFor(user));
        } catch (IOException e) {
            throw new AvatarNotFoundException(user.getUsername());
        }
    }

    public void delete(ApplicationUser user) {
        try {
            Files.deleteIfExists(avatarFileFor(user));
        } catch (IOException e) {
            // Best-effort cleanup; the row is the source of truth.
        }
    }

    private BufferedImage cropToCentredSquare(BufferedImage source) {
        int w = source.getWidth();
        int h = source.getHeight();
        int side = Math.min(w, h);
        int x = (w - side) / 2;
        int y = (h - side) / 2;
        return source.getSubimage(x, y, side, side);
    }

    private BufferedImage scaleTo(BufferedImage source, int size) {
        Image hint = source.getScaledInstance(size, size, Image.SCALE_SMOOTH);
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(hint, 0, 0, null);
        g.dispose();
        return out;
    }
}
