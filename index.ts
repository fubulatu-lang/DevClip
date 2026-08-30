import { AppRegistry } from 'react-native';
import { registerRootComponent } from 'expo';
import App from './App';
import OverlayApp from './src/OverlayApp';
import appJson from './app.json';

const appName = appJson.expo.name;

// The launcher app: the whole thing, full screen.
registerRootComponent(App);

// The floating overlay that OverlayService.kt mounts in its own window.
// A separate root from App: the overlay is mini or expanded, tethered to the
// bubble, and paste-only in mini — a different surface, not a resized one.
AppRegistry.registerComponent('DevClipPopup', () => OverlayApp);

export default appName;
