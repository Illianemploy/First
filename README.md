# UK Tobacco Prices

A sleek Android app that aggregates and provides real-time pricing data for cigarettes and rolling tobacco across UK retailers.

## Features

- **Real-time Price Updates**: View current prices from major UK retailers including Tesco, Sainsbury's, Co-op, Morrisons, and ASDA
- **Comprehensive Product Database**:
  - Popular cigarette brands (Marlboro, Richmond, Lambert & Butler, Benson & Hedges, and more)
  - Rolling tobacco products (Amber Leaf, Golden Virginia, Cutters Choice, Drum, Old Holborn, and more)
- **Advanced Filtering**:
  - Filter by product type (cigarettes or rolling tobacco)
  - Filter by retailer
  - Search by product name, brand, or retailer
- **Smart Sorting**: Sort by price, name, brand, retailer, or last updated
- **Price Per Unit**: Compare value with automatic price-per-cigarette or price-per-gram calculations
- **Modern UI**: Built with Jetpack Compose and Material 3 design
- **Dark Theme**: Eye-friendly dark mode interface

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
├── MainActivity.kt          # Main UI and Compose screens
├── TobaccoViewModel.kt      # Business logic and state management
├── TobaccoRepository.kt     # Data source with pricing information
└── TobaccoProduct.kt        # Data models
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

### Product Cards
Each product card displays:
- Product type badge (color-coded)
- Brand and product name
- Retailer name
- Current price
- Package size
- Price per unit (per cigarette or per gram)
- Last update timestamp

## Future Enhancements

Potential features for future versions:

- **Price History**: Track price changes over time with graphs
- **Favorites**: Save frequently searched products
- **Price Alerts**: Notifications when prices drop
- **Location-based**: Show nearby retailers
- **Barcode Scanner**: Quick product lookup
- **User Reviews**: Community ratings and reviews
- **API Integration**: Connect to real-time retailer price APIs
- **Promotions**: Highlight special offers and deals

## License

This project is for educational and informational purposes.

## Disclaimer

This app provides pricing information for legal tobacco products available in UK retail stores. All prices are for informational purposes only. Actual prices may vary by location and are subject to change. Users must be 18+ to purchase tobacco products in the UK.

## Contributing

Contributions are welcome! Please feel free to submit pull requests or open issues for bugs and feature requests.
