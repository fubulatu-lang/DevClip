import { AppRegistry } from 'react-native';
import { registerRootComponent } from 'expo';
import App from './App';
import PopupScreen from './src/screens/PopupScreen';
import appJson from './app.json';

const appName = appJson.expo.name;

// Normal full-screen app entry (MainActivity), used for the "Full App" state
// and for opening DevClip from the launcher directly.
registerRootComponent(App);

// A second, independent root component that OverlayService.kt mounts inside
// its own small WindowManager window (the floating bubble's popup). It reuses
// the exact same PopupScreen UI/state logic as the full app.
AppRegistry.registerComponent('DevClipPopup', () => PopupScreen);

export default appName;
