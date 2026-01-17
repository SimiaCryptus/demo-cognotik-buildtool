package com.simiacryptus;

import com.simiacryptus.cognotik.util.FileGenerator;
import com.simiacryptus.cognotik.util.UnifiedHarness;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static com.simiacryptus.CognotikUtils.relativize;

public class CodeReviewer {
    public static void main(String[] args) {
        UnifiedHarness.configurePlatform();
        String promptTemplate = args.length > 2 ? args[2] : "Update implementation file (%s) according to the standards documents";
        new FileGenerator() {}.run(
            args.length > 0 ? new File(args[0]) : new File("."),
            args.length > 1 ? new File(args[1]) : new File("src/main/java"),
            (root, folder) -> Arrays.stream(Objects.requireNonNull(folder.listFiles())).map(file -> relativize(root, file)).toList(),
            (source) -> source,
            FileGenerator.OverwriteModes.PatchExisting,
            (source) -> args.length > 3 ? Arrays.asList(args[3].split(",")) : List.of("docs/best_practices.md"),
            (source, target) -> promptTemplate.contains("%s") ? promptTemplate.replace("%s", target.toString()) : promptTemplate + " (" + target + ")",
            args.length > 4 ? Integer.parseInt(args[4]) : 4
        );
    }
}