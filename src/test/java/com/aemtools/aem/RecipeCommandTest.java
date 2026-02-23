package com.aemtools.aem;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class RecipeCommandTest {

    @Test
    void testRecipeCommandCanBeCreated() {
        RecipeCommand cmd = new RecipeCommand();
        assertNotNull(cmd);
    }

    @Test
    void testSiteLaunchRecipeCanBeCreated() {
        RecipeCommand.SiteLaunchRecipe recipe = new RecipeCommand.SiteLaunchRecipe();
        assertNotNull(recipe);
    }

    @Test
    void testContentBackupRecipeCanBeCreated() {
        RecipeCommand.ContentBackupRecipe recipe = new RecipeCommand.ContentBackupRecipe();
        assertNotNull(recipe);
    }

    @Test
    void testAssetBatchRecipeCanBeCreated() {
        RecipeCommand.AssetBatchRecipe recipe = new RecipeCommand.AssetBatchRecipe();
        assertNotNull(recipe);
    }

    @Test
    void testUserOnboardingRecipeCanBeCreated() {
        RecipeCommand.UserOnboardingRecipe recipe = new RecipeCommand.UserOnboardingRecipe();
        assertNotNull(recipe);
    }

    @Test
    void testPackageMigrateRecipeCanBeCreated() {
        RecipeCommand.PackageMigrateRecipe recipe = new RecipeCommand.PackageMigrateRecipe();
        assertNotNull(recipe);
    }

    @Test
    void testRecipeMainCommandReturnsZero() throws Exception {
        RecipeCommand cmd = new RecipeCommand();
        assertEquals(0, cmd.call());
    }
}
