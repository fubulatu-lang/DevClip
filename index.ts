import { registerRootComponent } from 'expo';
import App from './App';
import appJson from './app.json';

const appName = appJson.expo.name;

// The launcher app: the whole thing, full screen.
//
// There used to be a second root registered here, "DevClipPopup", which
// OverlayService mounted as a React surface inside the floating window. It is
// gone, and so is the bug it caused: a surface draws nothing without a live
// React instance behind it, and nothing starts one but an Activity — so the
// floating list came up as an empty outline until the launcher app had been
// opened once, and looked intermittent while being entirely deterministic.
// The list is drawn in native views now, by PopupListView.kt.
registerRootComponent(App);

export default appName;
