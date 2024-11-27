package com.mcdart.xp_synthesiser.items;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Objects;

import static com.mojang.serialization.Codec.BOOL;
import static net.minecraft.network.codec.ByteBufCodecs.INT;

public class KillRecorderData {

    public static final Codec<KillRecorderData> CODEC = RecordCodecBuilder.create(i -> i.group(
            BOOL.fieldOf("recording").forGetter(KillRecorderData::getRecording),
            Codec.INT.fieldOf("xp").forGetter(KillRecorderData::getXP),
            Codec.LONG.fieldOf("recordingStart").forGetter(KillRecorderData::getRecordingStart),
            Codec.LONG.fieldOf("recordingEnd").forGetter(KillRecorderData::getRecordingEnd)
    ).apply(i, KillRecorderData::new));

    public static final StreamCodec<ByteBuf, KillRecorderData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, KillRecorderData::getRecording,
            INT, KillRecorderData::getXP,
            ByteBufCodecs.VAR_LONG, KillRecorderData::getRecordingStart,
            ByteBufCodecs.VAR_LONG, KillRecorderData::getRecordingEnd,
            KillRecorderData::new
    );

    private boolean recording;
    private int xp;
    private long recordingStart;
    private long recordingEnd;

    public KillRecorderData(boolean recording, int xp, long recordingStart, long recordingEnd) {
        this.recording = recording;
        this.xp = xp;
        this.recordingStart = recordingStart;
        this.recordingEnd = recordingEnd;
    }

    public boolean getRecording() {
        return recording;
    }

    public int getXP() {
        return xp;
    }

    public long getRecordingStart() {
        return recordingStart;
    }

    public long getRecordingEnd() {
        return recordingEnd;
    }

    public void setRecording(boolean newRecording) {
        recording = newRecording;
    }
    public void setXP(int newXp) {
        xp = newXp;
    }
    public void setRecordingStart(long newRecordingStart) {
        recordingStart = newRecordingStart;
    }
    public void setRecordingEnd(long newRecordingEnd) {
        recordingEnd = newRecordingEnd;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        KillRecorderData that = (KillRecorderData) o;
        return recording == that.recording && xp == that.xp && recordingStart == that.recordingStart && recordingEnd == that.recordingEnd;
    }

    @Override
    public int hashCode() {
        return Objects.hash(recording, xp, recordingEnd, recordingStart);
//        return (int) ((recording ? 1 : 2) +
//                xp * 10 +
//                (recordingEnd - recordingStart) * 10000);
    }

}
