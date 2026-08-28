import { AppRegistry } from 'react-native';
import { registerRootComponent } from 'expo';
import App from './App';
import appJson from './app.json';

const appName = appJson.expo.name;

// Normal full-screen app entry (MainActivity), used for the "Full App" state
// and for opening DevClip from the launcher directly.
registerRootComponent(App);

// A second, independent root component that OverlayService.kt mounts inside
// its own small WindowManager window (the floating bubble's popup). It reuses
// the exact same App -> PopupScreen tree (including the font-loading gate)
// as the full app, so styling is identical in both places.
AppRegistry.registerComponent('DevClipPopup', () => App);

export default appName;
