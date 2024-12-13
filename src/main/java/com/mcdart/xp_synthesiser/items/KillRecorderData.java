package com.mcdart.xp_synthesiser.items;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import static net.minecraft.network.codec.ByteBufCodecs.INT;

public record KillRecorderData(boolean recording, int xp, long recordingStart, long recordingEnd) {

    public static final Codec<KillRecorderData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.fieldOf("recording").forGetter(KillRecorderData::recording),
            Codec.INT.fieldOf("xp").forGetter(KillRecorderData::xp),
            Codec.LONG.fieldOf("recordingStart").forGetter(KillRecorderData::recordingStart),
            Codec.LONG.fieldOf("recordingEnd").forGetter(KillRecorderData::recordingEnd)
    ).apply(i, KillRecorderData::new));

    public static final StreamCodec<ByteBuf, KillRecorderData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, KillRecorderData::recording,
            INT, KillRecorderData::xp,
            ByteBufCodecs.VAR_LONG, KillRecorderData::recordingStart,
            ByteBufCodecs.VAR_LONG, KillRecorderData::recordingEnd,
            KillRecorderData::new
    );

    public KillRecorderData addKill(int xp) {
        return new KillRecorderData(recording(), xp() + xp, recordingStart(), recordingEnd());
    }

    public static KillRecorderData createEmpty() {
        return new KillRecorderData(false, 0, 0L, 0L);
    }

}