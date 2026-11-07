package com.refresh165;
/** No package, player, cadence or playback state can select a lower ceiling. */
final class AdaptiveState {
    static int effective(boolean interactive,boolean ime){return !interactive?0:ime?60:165;}
}
