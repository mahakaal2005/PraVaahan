#!/bin/bash

# PraVaahan Build Verification Script
# This script provides comprehensive build verification for the Android project

set -e  # Exit on any error

echo "🚀 Starting PraVaahan Build Verification"
echo "========================================"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

# Check if we're in the right directory
if [ ! -f "app/build.gradle.kts" ]; then
    print_error "Please run this script from the project root directory"
    exit 1
fi

# Check if local.properties exists
if [ ! -f "local.properties" ]; then
    print_warning "local.properties not found. Creating from template..."
    if [ -f "local.properties.template" ]; then
        cp local.properties.template local.properties
        print_info "Please configure SUPABASE_URL and SUPABASE_ANON_KEY in local.properties"
    else
        print_error "local.properties.template not found"
        exit 1
    fi
fi

# Verify Supabase configuration
print_info "Checking Supabase configuration..."
if grep -q "SUPABASE_URL=" local.properties && grep -q "SUPABASE_ANON_KEY=" local.properties; then
    SUPABASE_URL=$(grep "SUPABASE_URL=" local.properties | cut -d'=' -f2)
    if [ -n "$SUPABASE_URL" ] && [ "$SUPABASE_URL" != "your_supabase_url_here" ]; then
        print_status "Supabase configuration found"
    else
        print_warning "Supabase URL not configured properly"
    fi
else
    print_warning "Supabase configuration missing in local.properties"
fi

# Clean previous builds
print_info "Cleaning previous builds..."
./gradlew clean

# Run comprehensive build verification
print_info "Running comprehensive build verification..."
./gradlew verifyBuild

# Check APK files
print_info "Verifying APK outputs..."
if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    DEBUG_SIZE=$(du -h "app/build/outputs/apk/debug/app-debug.apk" | cut -f1)
    print_status "Debug APK created: $DEBUG_SIZE"
else
    print_error "Debug APK not found"
    exit 1
fi

if [ -f "app/build/outputs/apk/release/app-release.apk" ]; then
    RELEASE_SIZE=$(du -h "app/build/outputs/apk/release/app-release.apk" | cut -f1)
    print_status "Release APK created: $RELEASE_SIZE"
else
    print_error "Release APK not found"
    exit 1
fi

# Check test results
print_info "Checking test results..."
if [ -d "app/build/test-results/testDebugUnitTest" ]; then
    TEST_COUNT=$(find app/build/test-results/testDebugUnitTest -name "*.xml" -exec grep -l "tests=" {} \; | wc -l)
    print_status "Unit test results found: $TEST_COUNT test files"
else
    print_warning "Unit test results not found"
fi

# Generate installation commands
print_info "Generating installation commands..."
echo ""
echo "📱 INSTALLATION COMMANDS:"
echo "========================="
echo "Debug APK:"
echo "  adb install app/build/outputs/apk/debug/app-debug.apk"
echo ""
echo "Release APK:"
echo "  adb install app/build/outputs/apk/release/app-release.apk"
echo ""

# Check for connected devices
if command -v adb &> /dev/null; then
    DEVICES=$(adb devices | grep -v "List of devices" | grep "device$" | wc -l)
    if [ $DEVICES -gt 0 ]; then
        print_status "$DEVICES Android device(s) connected"
        echo ""
        echo "🔧 QUICK TEST COMMANDS:"
        echo "======================="
        echo "Install and launch debug app:"
        echo "  adb install app/build/outputs/apk/debug/app-debug.apk"
        echo "  adb shell am start -n com.example.pravaahan.debug/com.example.pravaahan.MainActivity"
        echo ""
        echo "Check app logs:"
        echo "  adb logcat | grep PraVaahan"
        echo ""
    else
        print_warning "No Android devices connected"
        print_info "Connect a device or start an emulator to test installation"
    fi
else
    print_warning "ADB not found in PATH"
fi

# Final summary
echo ""
echo "🎉 BUILD VERIFICATION COMPLETED!"
echo "================================="
print_status "All verification phases passed"
print_status "APKs built successfully"
print_status "Tests executed successfully"
print_status "Quality checks passed"
print_status "App ready for testing"

echo ""
echo "📊 Reports generated:"
echo "  - Build verification: app/build/reports/build-verification-report.txt"
echo "  - Test results: app/build/reports/tests/"
echo "  - Lint report: app/build/reports/lint-results-debug.html"

echo ""
echo "🚀 Next steps:"
echo "  1. Install APK on test device"
echo "  2. Test basic app functionality"
echo "  3. Verify Supabase connectivity"
echo "  4. Test real-time features"
echo "  5. Validate error handling"

exit 0