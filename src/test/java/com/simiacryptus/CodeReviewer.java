package com.simiacryptus;

import com.simiacryptus.cognotik.util.FileGenerator;
import com.simiacryptus.cognotik.util.UnifiedHarness;

import java.io.File;
import java.util.Arrays;

import static com.simiacryptus.CognotikUtils.relativize;

public class CodeReviewer extends FileGenerator {
    public static void main(String[] args) {
        UnifiedHarness.Companion.configurePlatform();
        new CodeReviewer().run(
                new File("."),
                new File("src/main/java"),
                (root, folder) -> Arrays.stream(folder.listFiles()).map(file->relativize(root,file)).toList(),
                (source) -> source,
                FileGenerator.OverwriteModes.PatchExisting,
                (source) -> Arrays.asList(
                        "docs/best_practices.md"
                ),
                (source, target) -> "Update implementation file (" + target + ") according to the standards documents",
                4
        );
    }
}