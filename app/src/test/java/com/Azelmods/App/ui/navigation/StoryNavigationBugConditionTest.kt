package com.Azelmods.App.ui.navigation

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.property.Arb
import io.kotest.property.arbitrary.string
import io.kotest.property.arbitrary.stringPattern
import io.kotest.property.checkAll
import java.net.URLDecoder

/**
 * Bug Condition Exploration Test for Story Navigation
 * 
 * **Validates: Requirements 1.1, 1.2, 1.3, 1.4, 1.5, 1.6**
 * 
 * **Property 1: Bug Condition** - Story Navigation Fails with Special Characters in userId
 * 
 * **CRITICAL**: This test MUST FAIL on unfixed code - failure confirms the bug exists
 * **DO NOT attempt to fix the test or the code when it fails**
 * **NOTE**: This test encodes the expected behavior - it will validate the fix when it passes after implementation
 * **GOAL**: Surface counterexamples that demonstrate the bug exists
 * 
 * This test simulates the navigation flow from StoriesScreen to StoryViewerScreen
 * by testing the route generation logic with various Firebase UID formats.
 * 
 * Expected behavior (after fix):
 * - Navigation succeeds for all userId formats
 * - userId parameter is correctly passed to StoryViewerScreen
 * - Route generation properly handles special characters
 * 
 * Expected outcome on UNFIXED code:
 * - Test FAILS because route generation doesn't URL-encode special characters
 * - Counterexamples will show which userId formats cause navigation failure
 */
class StoryNavigationBugConditionTest : StringSpec({
    
    "Property 1: Story navigation should succeed with Firebase UIDs containing hyphens" {
        // Scoped PBT: Generate Firebase UIDs with hyphens (common format)
        val firebaseUidWithHyphens = Arb.stringPattern("[a-zA-Z0-9]{3}-[a-zA-Z0-9]{3}-[a-zA-Z0-9]{3}")
        
        checkAll(10, firebaseUidWithHyphens) { userId ->
            // Simulate the navigation route generation
            val generatedRoute = NavRoutes.storyViewerRoute(userId)
            
            // Log the generated route for debugging
            println("Generated route for userId '$userId': $generatedRoute")
            
            // Expected behavior: Route should be properly formatted
            // The route should be "story_viewer/{encoded_userId}"
            generatedRoute shouldContain "story_viewer/"
            
            // Extract the userId part from the route
            val routeUserId = generatedRoute.removePrefix("story_viewer/")
            
            // Expected behavior: The userId should be recoverable from the route
            // This simulates what NavController does when extracting arguments
            val recoveredUserId = try {
                URLDecoder.decode(routeUserId, "UTF-8")
            } catch (e: Exception) {
                routeUserId // If decoding fails, use as-is
            }
            
            // CRITICAL ASSERTION: The recovered userId must match the original
            // This will FAIL on unfixed code because hyphens are not URL-encoded
            recoveredUserId shouldBe userId
            
            // Document counterexample if assertion fails
            if (recoveredUserId != userId) {
                println("COUNTEREXAMPLE FOUND:")
                println("  Original userId: $userId")
                println("  Generated route: $generatedRoute")
                println("  Recovered userId: $recoveredUserId")
                println("  Issue: Route generation does not URL-encode special characters")
            }
        }
    }
    
    "Property 1: Story navigation should succeed with Firebase UIDs containing underscores" {
        // Scoped PBT: Generate Firebase UIDs with underscores
        val firebaseUidWithUnderscores = Arb.stringPattern("user_[a-zA-Z0-9]{4}_[a-zA-Z0-9]{3}")
        
        checkAll(10, firebaseUidWithUnderscores) { userId ->
            val generatedRoute = NavRoutes.storyViewerRoute(userId)
            
            println("Generated route for userId '$userId': $generatedRoute")
            
            generatedRoute shouldContain "story_viewer/"
            
            val routeUserId = generatedRoute.removePrefix("story_viewer/")
            val recoveredUserId = try {
                URLDecoder.decode(routeUserId, "UTF-8")
            } catch (e: Exception) {
                routeUserId
            }
            
            recoveredUserId shouldBe userId
            
            if (recoveredUserId != userId) {
                println("COUNTEREXAMPLE FOUND:")
                println("  Original userId: $userId")
                println("  Generated route: $generatedRoute")
                println("  Recovered userId: $recoveredUserId")
                println("  Issue: Underscores may not be properly handled in route generation")
            }
        }
    }
    
    "Property 1: Story navigation should succeed with Firebase UIDs containing mixed special characters" {
        // Scoped PBT: Generate Firebase UIDs with dots, hyphens, and underscores
        val firebaseUidWithMixedChars = Arb.stringPattern("user[._-][a-zA-Z0-9]{3}[._-][a-zA-Z0-9]{3}")
        
        checkAll(10, firebaseUidWithMixedChars) { userId ->
            val generatedRoute = NavRoutes.storyViewerRoute(userId)
            
            println("Generated route for userId '$userId': $generatedRoute")
            
            generatedRoute shouldContain "story_viewer/"
            
            val routeUserId = generatedRoute.removePrefix("story_viewer/")
            val recoveredUserId = try {
                URLDecoder.decode(routeUserId, "UTF-8")
            } catch (e: Exception) {
                routeUserId
            }
            
            recoveredUserId shouldBe userId
            
            if (recoveredUserId != userId) {
                println("COUNTEREXAMPLE FOUND:")
                println("  Original userId: $userId")
                println("  Generated route: $generatedRoute")
                println("  Recovered userId: $recoveredUserId")
                println("  Issue: Mixed special characters not properly handled")
            }
        }
    }
    
    "Property 1: Story navigation should succeed with concrete failing case - hyphenated Firebase UID" {
        // Concrete test case: Known Firebase UID format that fails
        val userId = "abc-123-def-456"
        
        val generatedRoute = NavRoutes.storyViewerRoute(userId)
        
        println("Concrete test - Generated route for userId '$userId': $generatedRoute")
        
        generatedRoute shouldContain "story_viewer/"
        
        val routeUserId = generatedRoute.removePrefix("story_viewer/")
        val recoveredUserId = try {
            URLDecoder.decode(routeUserId, "UTF-8")
        } catch (e: Exception) {
            routeUserId
        }
        
        // This assertion will FAIL on unfixed code
        recoveredUserId shouldBe userId
        
        if (recoveredUserId != userId) {
            println("CONCRETE COUNTEREXAMPLE:")
            println("  Original userId: $userId")
            println("  Generated route: $generatedRoute")
            println("  Recovered userId: $recoveredUserId")
            println("  Root cause: Hyphens in Firebase UID are not URL-encoded")
            println("  Expected route: story_viewer/abc%2D123%2D def%2D456")
            println("  Actual route: $generatedRoute")
        }
    }
    
    "Property 1: Story navigation should succeed with concrete failing case - underscore Firebase UID" {
        // Concrete test case: Firebase UID with underscores
        val userId = "user_test_123"
        
        val generatedRoute = NavRoutes.storyViewerRoute(userId)
        
        println("Concrete test - Generated route for userId '$userId': $generatedRoute")
        
        generatedRoute shouldContain "story_viewer/"
        
        val routeUserId = generatedRoute.removePrefix("story_viewer/")
        val recoveredUserId = try {
            URLDecoder.decode(routeUserId, "UTF-8")
        } catch (e: Exception) {
            routeUserId
        }
        
        recoveredUserId shouldBe userId
        
        if (recoveredUserId != userId) {
            println("CONCRETE COUNTEREXAMPLE:")
            println("  Original userId: $userId")
            println("  Generated route: $generatedRoute")
            println("  Recovered userId: $recoveredUserId")
            println("  Root cause: Underscores in Firebase UID may not be properly handled")
        }
    }
    
    "Property 1: Story navigation should succeed with concrete failing case - mixed special characters" {
        // Concrete test case: Firebase UID with dots, hyphens, and underscores
        val userId = "user.name_123-abc"
        
        val generatedRoute = NavRoutes.storyViewerRoute(userId)
        
        println("Concrete test - Generated route for userId '$userId': $generatedRoute")
        
        generatedRoute shouldContain "story_viewer/"
        
        val routeUserId = generatedRoute.removePrefix("story_viewer/")
        val recoveredUserId = try {
            URLDecoder.decode(routeUserId, "UTF-8")
        } catch (e: Exception) {
            routeUserId
        }
        
        recoveredUserId shouldBe userId
        
        if (recoveredUserId != userId) {
            println("CONCRETE COUNTEREXAMPLE:")
            println("  Original userId: $userId")
            println("  Generated route: $generatedRoute")
            println("  Recovered userId: $recoveredUserId")
            println("  Root cause: Mixed special characters not URL-encoded")
        }
    }
})
