# Vacation Tracker Android App

A comprehensive Android application for tracking your vacation itinerary, routes, and places visited during trips.

## Features

### ✈️ Core Functionality
- **Trip Management**: Start/end vacation trips with custom names
- **Real-time Location Tracking**: Continuous GPS tracking in background
- **Route Visualization**: See your traveled route on Google Maps
- **Places of Interest**: Mark and manage visited locations
- **Itinerary View**: Browse all places visited during your trip

### 🗺️ Maps Integration
- Google Maps integration with custom markers
- Route polylines showing your travel path
- Current location tracking
- Interactive place addition by tapping map

### 📊 Trip Statistics
- Total distance traveled
- Trip duration
- Number of places visited
- Real-time updates during active trips

### 🏛️ Data Management
- Local SQLite database using Room
- Offline functionality
- Trip history preservation
- Photo attachment support (basic implementation)

## Technical Architecture

### 🏗️ Architecture Pattern
- **MVVM (Model-View-ViewModel)** architecture
- **Repository Pattern** for data management
- **LiveData** for reactive UI updates
- **Coroutines** for asynchronous operations

### 🛠️ Technologies Used
- **Kotlin** - Primary language
- **Room Database** - Local data persistence
- **Google Maps SDK** - Maps and location services
- **Google Play Services Location** - GPS tracking
- **Foreground Service** - Background location tracking
- **ViewBinding** - Type-safe view references
- **Material Design 3** - Modern UI components

### 📁 Project Structure
```
app/src/main/java/com/vacationtracker/
├── activities/          # UI Activities
│   ├── MainActivity.kt
│   └── ItineraryActivity.kt
├── adapters/           # RecyclerView Adapters
│   └── PlaceAdapter.kt
├── database/           # Room Database
│   ├── VacationDatabase.kt
│   ├── Converters.kt
│   └── *Dao.kt
├── models/             # Data Models
│   ├── Trip.kt
│   ├── LocationPoint.kt
│   ├── Place.kt
│   └── Photo.kt
├── repositories/       # Data Repositories
│   ├── TripRepository.kt
│   ├── LocationRepository.kt
│   └── PlaceRepository.kt
├── services/           # Background Services
│   └── LocationTrackingService.kt
├── utils/              # Utility Classes
│   ├── PermissionUtils.kt
│   └── PhotoUtils.kt
└── viewmodels/         # ViewModels
    ├── MainViewModel.kt
    └── ItineraryViewModel.kt
```

## Setup Instructions

### 1. Prerequisites
- Android Studio Arctic Fox or newer
- Android SDK 24+ (Android 7.0)
- Google Play Services

### 2. Google Maps Setup
1. Get a Google Maps API key:
   - Go to [Google Cloud Console](https://console.cloud.google.com/)
   - Enable Maps SDK for Android
   - Create API key with Android restrictions

2. Add API key to project:
   - Create `local.properties` file in project root
   - Add: `MAPS_API_KEY=your_api_key_here`

### 3. Build Configuration
```bash
# Clone the project
git clone <repository-url>
cd VacationTracker

# Build the project
./gradlew build

# Install on device
./gradlew installDebug
```

### 4. Permissions Required
The app requires the following permissions:
- `ACCESS_FINE_LOCATION` - GPS location tracking
- `ACCESS_COARSE_LOCATION` - Network location
- `ACCESS_BACKGROUND_LOCATION` - Background tracking
- `FOREGROUND_SERVICE` - Continuous tracking
- `INTERNET` - Maps and geocoding
- `CAMERA` - Photo capture (optional)
- `WRITE_EXTERNAL_STORAGE` - Photo storage

## Usage Guide

### Starting a Trip
1. Open the app
2. Tap "Start Trip"
3. Enter trip name
4. Allow location permissions when prompted
5. The app will begin tracking your location

### Adding Places
- **Method 1**: Tap "Add Place" button to mark current location
- **Method 2**: Long-press on map to add place at specific location
- Enter place name and optional notes

### Viewing Itinerary
1. Tap "Itinerary" button
2. Browse all places visited
3. View details, notes, and visit times

### Ending a Trip
1. Tap "End Trip" in main screen
2. Confirm to stop tracking
3. Trip data is saved for future reference

## Database Schema

### Trips Table
- `id` (Primary Key)
- `name` - Trip name
- `startDate` - Trip start timestamp
- `endDate` - Trip end timestamp
- `isActive` - Current active status
- `totalDistance` - Distance traveled (meters)
- `totalDuration` - Trip duration (milliseconds)

### Location Points Table
- `id` (Primary Key)
- `latitude` - GPS latitude
- `longitude` - GPS longitude
- `timestamp` - Location timestamp
- `accuracy` - GPS accuracy
- `tripId` - Foreign key to trips

### Places Table
- `id` (Primary Key)
- `name` - Place name
- `latitude` - Place latitude
- `longitude` - Place longitude
- `visitedAt` - Visit timestamp
- `notes` - User notes
- `tripId` - Foreign key to trips

## Performance Considerations

### Battery Optimization
- Location updates every 30 seconds minimum
- Minimum distance change: 10 meters
- Uses foreground service for reliability
- Efficient database operations

### Memory Management
- LiveData for automatic lifecycle management
- Coroutines for non-blocking operations
- Proper bitmap handling for photos
- Database connection pooling

## Future Enhancements

### Planned Features
- [ ] Photo galleries for places
- [ ] Export trip data (GPX, KML)
- [ ] Social sharing capabilities
- [ ] Offline maps support
- [ ] Trip comparison analytics
- [ ] Weather data integration
- [ ] Expense tracking
- [ ] Custom place categories

### Technical Improvements
- [ ] Jetpack Compose migration
- [ ] Cloud backup integration
- [ ] Machine learning for place detection
- [ ] Advanced route optimization
- [ ] Better photo compression
- [ ] Geofencing for automatic place detection

## Troubleshooting

### Common Issues

**Location not tracking:**
- Check location permissions are granted
- Ensure GPS is enabled
- Verify Google Play Services is updated

**Maps not loading:**
- Verify API key is correctly configured
- Check internet connection
- Ensure Maps SDK is enabled in Google Cloud

**Background tracking stopped:**
- Check battery optimization settings
- Verify background app refresh is enabled
- Ensure foreground service permissions

### Debug Mode
Enable debug logging by setting:
```kotlin
// In VacationApplication.kt (if created)
BuildConfig.DEBUG = true
```

## Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Support

For support and questions:
- Create an issue on GitHub
- Email: support@vacationtracker.com
- Documentation: [Wiki](wiki-link)

---

**Happy Vacationing! 🏖️**