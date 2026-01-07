# UK Tobacco Prices

A premium, Uber-style Android app that aggregates and provides real-time pricing data for cigarettes and rolling tobacco across UK retailers. Features stunning 3D product visualizations, interactive gestures, and comprehensive brand histories.

## ✨ Key Features

### 🎨 Interactive 3D Product Viewer
- **Spin & Rotate**: Touch and drag to rotate products in 3D space
- **Pinch to Zoom**: Zoom in for detailed inspection (0.5x to 3x)
- **Drag to Move**: Pan around the product with intuitive gestures
- **Auto-Rotation**: Mesmerizing automatic rotation animation
- **Realistic Rendering**: Beautifully rendered cigarette packages and tobacco pouches with shadows, highlights, and 3D depth

### 📚 Brand Heritage & History
- **Interactive Timelines**: Explore 100+ years of tobacco brand history
- **Key Milestones**: Highlighted important events in brand evolution
- **Founder Stories**: Learn about the people behind iconic brands
- **Fun Facts**: Discover interesting trivia about each brand
- **7 Featured Brands**: Marlboro, Lambert & Butler, Richmond, Benson & Hedges, Amber Leaf, Golden Virginia, and Mayfair

### 💎 Premium Uber-Style Design
- **Sleek Dark Theme**: Sophisticated black and green color palette inspired by Uber
- **Smooth Animations**: Buttery 60fps transitions and micro-interactions
- **Material 3**: Latest Material Design guidelines with custom theming
- **Enhanced Typography**: Clean, modern font hierarchy for maximum readability
- **Polished UI**: Rounded corners, gradient accents, and elevated cards

### 📊 Real-time Price Tracking
- **Live Updates**: Prices refresh automatically every 30 seconds
- **150+ Products**: Comprehensive database of cigarettes and rolling tobacco
- **6 Major Retailers**: Tesco, Sainsbury's, Co-op, Morrisons, ASDA, Local Shops
- **Price Trends**: Visual indicators showing price changes
- **Per-Unit Pricing**: Instant calculation of price per cigarette or gram

### 🔍 Advanced Search & Filtering
- **Smart Search**: Real-time search across products, brands, and retailers
- **Multi-Filter**: Combine type, retailer, and sorting options
- **Active Filter Count**: Visual feedback on applied filters
- **Quick Clear**: Reset all filters with one tap

### ⭐ Favorites System
- **Bookmark Products**: Save your favorite products for quick access
- **Persistent Storage**: Favorites sync across app sessions
- **Animated Interactions**: Satisfying heart animation when favoriting
- **Quick Access**: Dedicated Favorites tab in bottom navigation

### 🎯 Modern Navigation
- **Bottom Navigation**: Easy access to Home, Brands, Favorites, and Settings
- **Smooth Transitions**: Page transitions with fade and slide animations
- **Deep Linking**: Direct navigation to products and brand histories
- **Smart Back Stack**: Intelligent navigation state management

## Technology Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM (Model-View-ViewModel)
- **State Management**: Kotlin Flows and StateFlow
- **Minimum SDK**: Android 8.0 (API 26)
- **Target SDK**: Android 14 (API 34)

## Project Structure

```
app/src/main/java/com/uktobacco/
├── MainActivity.kt                      # Main entry point with bottom navigation
├── TobaccoViewModel.kt                  # Business logic and state management
├── TobaccoRepository.kt                 # Data source with pricing information
├── TobaccoProduct.kt                    # Product data models
├── data/
│   └── CompanyHistory.kt                # Brand history data and repository
├── navigation/
│   └── Navigation.kt                    # Navigation graph and routes
├── ui/
│   ├── theme/
│   │   ├── Color.kt                     # Uber-inspired color palette
│   │   ├── Typography.kt                # Modern typography system
│   │   └── Theme.kt                     # Material 3 theme configuration
│   ├── components/
│   │   ├── Product3DViewer.kt           # Interactive 3D product visualization
│   │   ├── EnhancedProductCard.kt       # Polished product cards with animations
│   │   └── LoadingEffects.kt            # Shimmer and loading states
│   └── screens/
│       ├── HomeScreen.kt                # Main product listing
│       ├── ProductDetailScreen.kt       # Detailed view with 3D viewer
│       ├── CompanyHistoryScreen.kt      # Brand timeline and history
│       ├── BrandsScreen.kt              # Brand directory grid
│       ├── FavoritesScreen.kt           # Saved products
│       └── SettingsScreen.kt            # App preferences
```

## Building the App

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- JDK 17 or later
- Android SDK with API 34

### Build Instructions

1. Clone the repository:
   ```bash
   git clone <repository-url>
   cd First
   ```

2. Open the project in Android Studio

3. Sync Gradle files:
   ```bash
   ./gradlew build
   ```

4. Run on an emulator or physical device:
   ```bash
   ./gradlew installDebug
   ```

### Build from Command Line

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease
```

The APK will be generated in `app/build/outputs/apk/`

## App Screenshots

### Main Features

- **Product Listing**: View all tobacco products with prices, retailers, and last update times
- **Search**: Quickly find specific products, brands, or retailers
- **Filters**: Toggle between cigarettes and rolling tobacco, filter by specific retailers
- **Price Comparison**: See price per cigarette or price per gram for easy comparison
- **Real-time Updates**: Prices automatically update to reflect current market data

## Data Source

The app currently uses a comprehensive mock dataset with realistic UK tobacco pricing. The data includes:

- 15+ popular cigarette brands across multiple retailers
- 18+ rolling tobacco products in various sizes (30g, 40g, 50g)
- Price variations between retailers
- Real-time price simulation with periodic updates

For production use, this can be replaced with actual API integrations from:
- Retailer price APIs
- Price comparison services
- Web scraping implementations

## Features in Detail

### Search Functionality
- Search across product names, brands, and retailers
- Real-time results as you type
- Clear button for quick reset

### Filtering
- **Product Type**: All, Cigarettes only, or Rolling Tobacco only
- **Retailer**: Filter to see prices from specific stores
- **Quick Clear**: Reset all filters with one tap

### Sorting Options
- Price: Low to High
- Price: High to Low
- Name: A to Z
- Brand
- Retailer
- Last Updated (newest first)

### Enhanced Product Cards
Each product card features:
- **Gradient Accent**: Color-coded top border (blue for cigarettes, orange for tobacco)
- **Favorite Button**: Animated heart icon with bounce effect
- **Price Trends**: Live indicators showing price changes
- **Smart Badges**: Product type, retailer, and update status
- **Press Animation**: Card scales down with spring physics when pressed
- **Recent Updates**: Pulsing dot for products updated in last 10 minutes
- **Value Metrics**: Prominent display of total price and per-unit cost

### 3D Product Viewer
Revolutionary interactive product visualization:
- **Gesture Controls**:
  - Rotate: Drag finger to spin products 360°
  - Zoom: Pinch gesture for 0.5x to 3x magnification
  - Pan: Two-finger drag to move product around screen
- **Auto-Rotation**: Elegant 20-second rotation loop when idle
- **Realistic 3D**: Custom-drawn 3D cigarette packages and tobacco pouches
- **Dynamic Lighting**: Highlights, shadows, and depth effects
- **Material Details**: Zipper seals, brand labels, and texture lines
- **Live Feedback**: Real-time scale indicator and gesture hints

### Brand History Timelines
Immersive brand storytelling:
- **Chronological Events**: Year-by-year milestones from founding to present
- **Visual Timeline**: Connected nodes with color-coded importance
- **Key Milestones**: Special highlighting for pivotal moments
- **Founder Information**: Details about brand creators
- **Company Facts**: Curated interesting trivia
- **Smooth Animations**: Events fade and slide in as you scroll

## 🎯 App Screens

### Home
- Product grid with enhanced cards
- Live search with instant results
- Multi-criteria filtering (type, retailer)
- 6 sorting options
- Active filter indicators
- Shimmer loading states

### Product Detail
- Full-screen 3D product viewer
- Interactive gesture controls
- Comprehensive product information
- Price breakdown and analysis
- Quick access to brand history
- Favorite toggle with animation
- Share functionality

### Brands
- Grid layout of all featured brands
- Brand logo with initial
- Founded year and heritage
- Color-coded brand identity
- Direct navigation to full history

### Company History
- Hero card with brand information
- Interactive timeline with events
- Milestone highlighting
- Founder and headquarters details
- Years of heritage calculation
- Fun facts section

### Favorites
- All saved products in one place
- Same enhanced card design
- Quick unfavorite option
- Empty state with helpful message
- Real-time sync with home screen

### Settings
- Notification preferences
- Auto-refresh toggle
- Display customization
- Data management
- Version and legal information

## 🚀 Technical Highlights

### Architecture
- **MVVM Pattern**: Clear separation of concerns
- **Unidirectional Data Flow**: StateFlow for reactive UI
- **Navigation Component**: Type-safe navigation with animations
- **Jetpack Compose**: 100% declarative UI
- **Material 3**: Latest design system implementation

### Performance
- **Lazy Loading**: Efficient list rendering
- **State Hoisting**: Optimized recomposition
- **Animation Performance**: 60fps smooth animations
- **Memory Efficient**: Proper lifecycle management

### Dependencies
- Jetpack Compose BOM 2024.01.00
- Navigation Compose 2.7.6
- Accompanist (System UI, Pager)
- Coil for image loading
- DataStore for preferences
- Kotlin Coroutines 1.7.3

## Future Enhancements

Potential features for future versions:

- **Price History Graphs**: Track price changes over time with interactive charts
- **Price Alerts**: Push notifications when prices drop below threshold
- **Location-based**: Show nearby retailers on map
- **Barcode Scanner**: Quick product lookup via camera
- **User Reviews**: Community ratings and reviews
- **Live API Integration**: Connect to real-time retailer price APIs
- **Promotions**: Highlight special offers and deals
- **Comparison Mode**: Side-by-side product comparison
- **Price Predictions**: ML-based price trend forecasting
- **Augmented Reality**: AR product placement and visualization

## License

This project is for educational and informational purposes.

## Disclaimer

This app provides pricing information for legal tobacco products available in UK retail stores. All prices are for informational purposes only. Actual prices may vary by location and are subject to change. Users must be 18+ to purchase tobacco products in the UK.

## Contributing

Contributions are welcome! Please feel free to submit pull requests or open issues for bugs and feature requests.
