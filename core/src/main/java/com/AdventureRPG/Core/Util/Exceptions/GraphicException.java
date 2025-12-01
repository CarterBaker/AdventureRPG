package com.AdventureRPG.Core.Util.Exceptions;

public final class GraphicException {

    // OpenGL \\

    public static class OpenGLException extends ExceptionEngine {

        public OpenGLException(String message) {
            super(message);
        }

        public OpenGLException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    // Texture Manager \\

    public static class ImageReadException extends ExceptionEngine {

        public ImageReadException(String message) {
            super(message);
        }

        public ImageReadException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class ImageSizeException extends ExceptionEngine {

        public ImageSizeException(String message) {
            super(message);
        }

        public ImageSizeException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class TextureAliasNotFoundException extends ExceptionEngine {

        public TextureAliasNotFoundException(String message) {
            super(message);
        }

        public TextureAliasNotFoundException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class TextureAliasException extends ExceptionEngine {

        public TextureAliasException(String message) {
            super(message);
        }

        public TextureAliasException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class ShaderProgramException extends ExceptionEngine {

        public ShaderProgramException(String message) {
            super(message);
        }

        public ShaderProgramException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
