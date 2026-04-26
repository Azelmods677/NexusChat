#!/bin/bash

# Script to replace all hardcoded purple colors with MaterialTheme.colorScheme references
# This script will be used as a reference for manual fixes

echo "Finding all hardcoded purple color instances..."

# Common purple hex values to replace
declare -A color_map=(
    ["Color(0xFF7C3AED)"]="MaterialTheme.colorScheme.primary"
    ["Color(0xFF6200EE)"]="MaterialTheme.colorScheme.primary"
    ["Color(0xFF8B5CF6)"]="MaterialTheme.colorScheme.primary"
    ["Color(0xFF9333EA)"]="MaterialTheme.colorScheme.primary"
    ["Color(0xFF7B2FBE)"]="MaterialTheme.colorScheme.primary"
    ["Color(0xFF6D28D9)"]="MaterialTheme.colorScheme.primary"
    ["Color(0xFFBB86FC)"]="MaterialTheme.colorScheme.primaryContainer"
    ["Color(0xFFE9D5FF)"]="MaterialTheme.colorScheme.primaryContainer"
)

# Files to process (excluding build directories and theme files)
find app/src/main/java -name "*.kt" -not -path "*/build/*" -not -path "*/theme/*" | while read file; do
    echo "Processing: $file"
    
    # Check if file contains any hardcoded colors
    if grep -q "Color(0xFF7C3AED)\|Color(0xFF6200EE)\|Color(0xFF8B5CF6)" "$file"; then
        echo "  Found hardcoded colors in $file"
    fi
done

echo "Done!"
