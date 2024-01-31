SwingFluidSwipeAPI
========

Extend the Swing framework with fluid-swipe gesture support. Our aim is to reduce the gap between native applications and AWT/Swing applications, providing a more consistent and pleasant UI/UX with animated feedback.

### macOS/OS X case
Support for the fluid-swipe gesture that is largely used for navigation in many AppKit-built programs, notably Safari, Xcode and Photos, is not offered by JDK – in fact, as far as swipe gestures are concerned, the JDK supports only the discrete ones (namely, 3-finger swipe gestures via  `com.apple.eawt.event`)

[demovideolink]

## Usage
To start event monitoring:
````java
FluidSwipe.startEventMonitoring();
````
To stop instead event monitoring:
````java
FluidSwipe.stopEventMonitoring();
````

### Adding listeners and blocking unwanted incoming gestures

````java

import javax.swing.*;

public class NavigableComponent extends JComponent implements FluidSwipeVetoer {
  // [...]
  protected void configGestures() {
    FluidSwipe.addListenerTo(this, new FluidSwipeAdapter() {
      @Override
      public void fluidSwipeEnded(final FluidSwipeEvent e) {
        if (e.getGestureState() == FluidSwipeEvent.State.SUCCESS) {
          if (contentDirection == FluidSwipeEvent.Direction.LEFT_TO_RIGHT)
            this.navigateBack();
          else this.navigateForward();
        }
      }
    });
  }

  // [...]
  @Override
  public boolean permitFluidSwipeGesture(final FluidSwipeEvent e) {
    if (e.getLogicalGestureDirection() == FluidSwipeEvent.Direction.LEFT_TO_RIGHT)
      return this.canNavigateBack();
    else return this.canNavigateForward();
  }
}
````


A runnable sample is also provided [here]

### OS settings (macOS/OS X)
In order for fluid-swipe gesture to work, the following conditions must be met, in normal circumstances:
- If using a supported trackpad:
  - `System Preferences`/`System Settings` > `Accessibility` > `Pointer Control`/`Mouse & Trackpad` > `Trackpad Options...` > `Use trackpad for scrolling`/`Scrolling` is flagged on
  - `System Preferences`/`System Settings` > `Trackpad` > `More Gestures` > `Swipe with Two or Three Fingers` or `Scroll Left or Right with Two Fingers` is selected
- If using a mouse such a Magic Mouse:
  - `System Preferences`/`System Settings` > `Accessibility` > `Pointer Control`/`Mouse & Trackpad` > `Mouse Options...` > `Use mouse for scrolling`/`Scrolling` is flagged on
  - `System Preferences`/`System Settings` > `Trackpad` > `More Gestures` > `Swipe with One or Two Fingers` or `Scroll Left or Right with One Finger` is selected

### Glossary
- "veto a fluid-swipe": consists in preventing the performed physical swipe gesture from being interpreted as fluid-swipe, in which case this gesture will be handled as usual (i.e., as a horizontal scroll gesture, thus a set of `MouseWheelEvent` will be dispatched).
- "swipeable component": a Swing component enabled to receive fluid-swipe events, i.e., at least a proper listener has been attached to this component via `FluidSwipe#addListenerTo`
- "vetoer component": a Swing component that implements `FluidSwipeVetoer`; it has the facility to veto fluid-swipe event.

### Avoiding common pitfalls
#### Conflicting gestures — implementation of a scroll/fluid-swipe coexistence mechanism

The physical gesture that may start a fluid-swipe can also be interpreted as a horizontal scroll gesture.
It is **absolutely recommended** that components listening for scroll-wheel events, notably  `JScrollPane`, implement `FluidSwipeVetoer`, in order to avoid unexpected behaviours.
Ideally, fluid-swipe gesture should have a lower priority than scroll event. To achieve that, veto the fluid-swipe when it is appropriate.
We strongly encourage you to consider the following example involving a `JScrollPane`.
```java
public class FluidSwipeAwareJScrollPane extends JScrollPane implements FluidSwipeVetoer {
    // [...]
    @Override
    public boolean permitFluidSwipeGesture(FluidSwipeEvent e) {
      final FluidSwipeEvent.Direction contentDirection = e.getLogicalGestureDirection();
      if (horizontalScrollBar == null || getHorizontalScrollBarPolicy() == HORIZONTAL_SCROLLBAR_NEVER) return true;
      return (contentDirection == FluidSwipeEvent.Direction.RIGHT_TO_LEFT && (horizontalScrollBar.getValue() + horizontalScrollBar.getModel().getExtent()) == horizontalScrollBar.getMaximum())
              || (contentDirection == FluidSwipeEvent.Direction.LEFT_TO_RIGHT && horizontalScrollBar.getValue() == horizontalScrollBar.getMinimum());
    }
}
```
Please be also aware that not to veto an incoming fluid-swipe results in `MouseWheelEvent` not to be dispatching as expected.

## HW/SW Requirements
- OS:macOS/OS X >= 10.13.
- Hardware: Apple Magic Trackpad, Mac laptop's built-in trackpad et similia. Apple Magic Mouse is expected to work, but it has not yet been tested — unfortunately, we do not own this device.

## Contribute
If any problem is encountered, please submit an issue and eventually propose a solution.
### Guidelines

Ensure to provide also:
- information about the running machine and environment (which version of macOS/OS X, JDK, architecture)
- steps to reproduce the malfunction (ideally an [SCCE](http://sscce.org))
- a brief but complete written description of the issue.

Avoid please:
- The use of sexualized language or imagery and unwelcome sexual attention or advances
- Trolling, insulting/derogatory comments, and personal or political attacks
- Public or private harassment
- Publishing others' private information, such as a physical or electronic address, without explicit permission
- Other conducts which could reasonably be considered inappropriate in a professional setting

Any content deemed as abuse or spam will be reported to GitHub.

### why not?
- ensure that Magic Mouse works flawlessly — testers are welcome
- add other platform support


## License
This project is licensed under the [Apache 2.0 license]().

