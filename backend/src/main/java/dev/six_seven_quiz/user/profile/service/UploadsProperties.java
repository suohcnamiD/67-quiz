package dev.six_seven_quiz.user.profile.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.uploads")
public class UploadsProperties {
    /** Directory under which user-uploaded files live. Relative paths are resolved against the working dir. */
    private String dir = "./uploads";

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }
}
