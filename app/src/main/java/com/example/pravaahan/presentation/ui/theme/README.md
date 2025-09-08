# PraVaahan Material Design 3 Theme System

This document describes the Material Design 3 theme implementation for the PraVaahan railway control application.

## Overview

The PraVaahan theme system is built on Material Design 3 principles, optimized for railway control environments with:
- High contrast colors for visibility in various lighting conditions
- Professional color palette suitable for critical infrastructure
- Comprehensive semantic colors for train statuses and priorities
- Support for light and dark themes with persistent user preferences
- Accessibility compliance with proper contrast ratios

## Architecture

### Core Components

1. **Color System** (`Color.kt`)
   - Complete Material Design 3 color tokens
   - Railway-specific semantic colors
   - Status and priority color schemes

2. **Typography** (`Type.kt`)
   - Enhanced readability for control environments
   - Complete Material Design 3 typography scale
   - Optimized for mobile screens

3. **Shapes** (`Shape.kt`)
   - Professional rounded corner system
   - Consistent with Material Design 3 guidelines

4. **Theme Management** (`ThemeManager.kt`)
   - Persistent theme preferences using DataStore
   - Reactive theme switching
   - System theme detection

## Color Palette

### Primary Colors
- **Light Theme**: Professional blue (#005AC1) - conveys trust and reliability
- **Dark Theme**: Light blue (#A8C7FA) - maintains accessibility in dark mode

### Semantic Colors

#### Train Status Colors
- **On Time**: Green (#2E7D32) - indicates normal operations
- **Delayed**: Amber (#EF6C00) - indicates minor issues
- **At Risk**: Orange (#D84315) - indicates potential problems
- **Conflict**: Red (#D32F2F) - indicates critical issues requiring immediate attention
- **Maintenance**: Blue (#1976D2) - indicates scheduled maintenance

#### Priority Colors
- **Express**: Purple (#7B1FA2) - highest priority trains
- **High**: Red (#D32F2F) - high priority passenger trains
- **Medium**: Orange (#FF8F00) - regular passenger trains
- **Low**: Green (#388E3C) - freight and local trains

## Usage

### Basic Theme Setup

```kotlin
@Composable
fun MyApp() {
    PravahanTheme {
        // Your app content
    }
}
```

### Theme Switching

```kotlin
@Composable
fun MyScreen(themeManager: ThemeManager = hiltViewModel()) {
    val scope = rememberCoroutineScope()
    
    Button(
        onClick = {
            scope.launch {
                themeManager.toggleTheme()
            }
        }
    ) {
        Text("Toggle Theme")
    }
}
```

### Using Semantic Colors

```kotlin
@Composable
fun TrainStatusIndicator(status: TrainStatus) {
    val color = when (status) {
        TrainStatus.ON_TIME -> OnTimeGreen
        TrainStatus.DELAYED -> DelayedAmber
        TrainStatus.EMERGENCY -> ConflictRed
        // ... other statuses
    }
    
    Box(
        modifier = Modifier
            .size(12.dp)
            .background(color, CircleShape)
    )
}
```

## Components

### Pre-built Components

1. **TrainStatusChip** - Displays train status with appropriate colors
2. **TrainPriorityChip** - Shows train priority with color coding
3. **TrainCard** - Complete train information card
4. **ConflictAlertCard** - Critical conflict alerts with action buttons
5. **LoadingIndicator** - Consistent loading states
6. **ErrorMessage** - User-friendly error displays

### Component Usage

```kotlin
@Composable
fun TrainList(trains: List<Train>) {
    LazyColumn {
        items(trains) { train ->
            TrainCard(
                train = train,
                onClick = { /* handle click */ }
            )
        }
    }
}
```

## Accessibility

The theme system ensures:
- Minimum 4.5:1 contrast ratio for normal text
- Minimum 3:1 contrast ratio for large text
- Color is not the only means of conveying information
- Support for system accessibility settings

## Testing

Theme components are tested for:
- Color consistency across light/dark themes
- Proper semantic color usage
- Accessibility compliance
- Component rendering in different theme states

Run theme tests:
```bash
./gradlew testDebugUnitTest
```

## Best Practices

1. **Always use theme colors**: Access colors through `MaterialTheme.colorScheme`
2. **Semantic colors for status**: Use predefined status colors for consistency
3. **Test in both themes**: Ensure components work in light and dark modes
4. **Accessibility first**: Consider users with visual impairments
5. **Consistent spacing**: Use theme shapes and typography scales

## Migration from Material 2

If migrating from Material 2:
1. Replace `Colors` with `ColorScheme`
2. Update color references (e.g., `primary` remains the same)
3. Use new container colors for better hierarchy
4. Update shape system to use new tokens
5. Test thoroughly in both light and dark themes

## Future Enhancements

Planned improvements:
- Dynamic color support for Android 12+
- Custom font integration
- Additional semantic colors for new features
- Enhanced accessibility features
- Theme customization for different railway zones