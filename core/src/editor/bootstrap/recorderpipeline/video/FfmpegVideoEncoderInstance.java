package editor.bootstrap.recorderpipeline.video;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import editor.runtime.EditorSetting;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class FfmpegVideoEncoderInstance extends VideoEncoderInstance {

    /*
     * Writes a video through an installed ffmpeg. The process reads raw
     * 24-bit frames on its input at the export's size and rate and encodes
     * them with the format's own arguments — H.264 or ProRes — while its
     * messages go to a log beside the video, kept only when it fails.
     */

    // Process
    private File executable;
    private String[] formatArguments;
    private Process process;
    private OutputStream processInput;
    private File logFile;

    // Constructor \\

    public void constructor(File executable, String[] formatArguments) {
        this.executable = executable;
        this.formatArguments = formatArguments;
    }

    // Management \\

    @Override
    protected boolean openFile() {

        this.logFile = new File(file.getParentFile(), file.getName() + EditorSetting.RECORDER_FFMPEG_LOG_SUFFIX);

        ProcessBuilder builder = new ProcessBuilder(buildCommand());
        builder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        builder.redirectError(logFile);

        try {
            this.process = builder.start();
            this.processInput = process.getOutputStream();
            return true;
        } catch (IOException e) {
            fail(EditorSetting.RECORDER_MESSAGE_FFMPEG_START + e.getMessage());
            return false;
        }
    }

    private ObjectArrayList<String> buildCommand() {

        ObjectArrayList<String> command = new ObjectArrayList<>();
        command.add(executable.getAbsolutePath());

        for (String argument : EditorSetting.RECORDER_FFMPEG_INPUT_ARGUMENTS)
            command.add(argument);

        command.add(EditorSetting.RECORDER_FFMPEG_SIZE_FLAG);
        command.add(width + EditorSetting.RECORDER_FFMPEG_SIZE_SEPARATOR + height);
        command.add(EditorSetting.RECORDER_FFMPEG_RATE_FLAG);
        command.add(Integer.toString(frameRate));
        command.add(EditorSetting.RECORDER_FFMPEG_INPUT_FLAG);
        command.add(EditorSetting.RECORDER_FFMPEG_STANDARD_INPUT);

        for (String argument : formatArguments)
            command.add(argument);

        command.add(file.getAbsolutePath());
        return command;
    }

    @Override
    public void encodeFrame(byte[] frame) {

        if (hasFailed())
            return;

        try {
            processInput.write(frame);
        } catch (IOException e) {
            fail(EditorSetting.RECORDER_MESSAGE_FFMPEG_WRITE + logFile.getAbsolutePath());
        }
    }

    @Override
    public boolean close() {

        try {
            processInput.close();

            int exitCode = process.waitFor();

            if (exitCode != 0)
                fail(EditorSetting.RECORDER_MESSAGE_FFMPEG_EXIT + exitCode + EditorSetting.RECORDER_MESSAGE_SEE_LOG
                        + logFile.getAbsolutePath());
        } catch (IOException e) {
            fail(EditorSetting.RECORDER_MESSAGE_FFMPEG_WRITE + logFile.getAbsolutePath());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail(EditorSetting.RECORDER_MESSAGE_INTERRUPTED);
        }

        if (!hasFailed())
            logFile.delete();

        return !hasFailed();
    }

    @Override
    public void abort() {

        if (process == null)
            return;

        process.destroyForcibly();
        logFile.delete();
    }
}
