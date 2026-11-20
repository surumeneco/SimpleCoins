package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * A04 local, unpublished ownership candidate from read-only SAVED BlockLocker
 * observations. The four source/scope predicates are independent external
 * proofs; they MUST NOT be inferred from a successful per-chest NBT read.
 * Never publishes settlement, registers players or changes Admission.
 */
public final class SavedBlockLockerOwnerCandidate {
    private SavedBlockLockerOwnerCandidate() {}

    public record ExternalProof(
            boolean savedTerrainSourceVerified,
            boolean allApplicableSignsVerified,
            boolean physicalStorageDeduplicated,
            boolean sameSavedSourceGenerationVerified,
            boolean nonProtectionVerified) {
        public static ExternalProof unknown() {
            return new ExternalProof(false,false,false,false,false);
        }
    }

    public record Candidate(BlockLockerSavedOwnerVerdict.Verdict verdict,
                            Optional<UUID> verifiedOwner) {
        public Candidate {
            Objects.requireNonNull(verdict);
            Objects.requireNonNull(verifiedOwner);
            if ((verdict == BlockLockerSavedOwnerVerdict.Verdict.OWNER_UNPUBLISHED)
                    != verifiedOwner.isPresent())
                throw new IllegalArgumentException("Only OWNER_UNPUBLISHED may retain a verified UUID");
        }
    }

    public static Candidate fromObservedSigns(
            SavedBlockLockerChestSigns.Observation observed,
            ExternalProof externalProof) throws IOException {
        if (externalProof == null)
            throw new IOException("Independent SAVED source evidence is missing");
        SavedBlockLockerSignSummary.Summary summary =
                SavedBlockLockerSignSummary.summarize(observed);
        var evidence = new BlockLockerSavedOwnerVerdict.Evidence(
                externalProof.savedTerrainSourceVerified(),
                externalProof.allApplicableSignsVerified(),
                externalProof.physicalStorageDeduplicated(),
                externalProof.sameSavedSourceGenerationVerified(),
                summary.protectionSignObserved(),
                !summary.protectionSignObserved() && externalProof.nonProtectionVerified(),
                summary.verifiedMainOwner().isPresent(),
                summary.everyoneEffective(),
                summary.everyoneAbsent(),
                summary.mainOwnerConflict() || (summary.protectionSignObserved()
                        && externalProof.nonProtectionVerified()));
        var verdict = BlockLockerSavedOwnerVerdict.decide(evidence);
        return new Candidate(verdict,
                verdict == BlockLockerSavedOwnerVerdict.Verdict.OWNER_UNPUBLISHED
                        ? summary.verifiedMainOwner() : Optional.empty());
    }

    /**
     * Full local read chain from explicitly selected SAVED Chunks. This is
     * NOT a validator of all selected world roots, live writers or global
     * generation; the external witness still controls any positive verdict.
     */
    public static Candidate fromSavedChunks(
            Map<SavedBlockLockerChestSigns.ChunkPosition,Map<String,Object>> selectedChunks,
            SavedBlockLockerChestSigns.Position chest,
            Set<String> privateHeaders,
            Set<String> moreHeaders,
            Set<String> everyoneAliases,
            BlockLockerSavedProfileResolution.DisplayDecoder decoder,
            ExternalProof externalProof) throws IOException {
        return fromObservedSigns(
                SavedBlockLockerChestSigns.observe(selectedChunks,chest,
                        privateHeaders,moreHeaders,everyoneAliases,decoder),
                externalProof);
    }
}
