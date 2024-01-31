/*
 * Copyright 2024 Giuliano Gorgone
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package eu.giulianogorgone.fluidswipe.components;

import eu.giulianogorgone.fluidswipe.event.FluidSwipeEvent;

import java.awt.*;

/**
 * Can be used for providing to navigable components pleasing UI feedback in response to a fluid-swipe event.
 * Typically, as the fingers swipe onto the input surface, the navigable pages will move on the screen accordingly.
 * To do so, firstly, an image of the page to be animated is captured; secondly, the image can be processed as desired and drawn.
 * Also, an image can be displayed beneath the animated page.
 * The animation painting process should be delegated to an {@code AnimPainterDelegate} instance.
 *
 * @author Giuliano Gorgone (anticleiades)
 * @see AnimPainterDelegate
 */
public interface NavigationSwipeAnimSupport {

    /**
     * Returns the image to be drawn beneath the animated page – typically this is the image of the component containing the animated page.
     *
     * @param e the fluid-swipe event
     * @return an image to be drawn beneath the animated page; {@code null} if any image must not be displayed beneath the animated page.
     */
    Image getBackgroundImage(final FluidSwipeEvent e);

    /**
     * This method returns an image of the page intended to be shown once the fluid-swipe gesture
     * successfully completes.
     *
     * @param e the fluid-swipe event
     * @return the page intended to be shown once the fluid-swipe gesture successfully completes; {@code null}, if no such page exists.
     * For instance, if user, when page <code>n</code> is displayed,
     * triggers a swipe to navigate back, this method could return an image of the <code>n-1</code><sup>th</sup> page (if existing);
     * this page will eventually, if gesture ends successfully, replace the <code>n</code><sup>th</sup> page.
     * While user is swiping, the <code>n</code><sup>th</sup> page moves on the screen accordingly.
     */
    Image getDestinationPage(final FluidSwipeEvent e);

    /**
     * This method returns an image of the page from which navigation starts; this page may be the active page prior to the activation of the fluid-swipe gesture.
     *
     * @param e the fluid-swipe event
     * @return image of the page from which navigation starts
     */
    Image getPageToNavFrom(final FluidSwipeEvent e);

    /**
     * This method returns the bounds of a page.
     *
     * @return the bounds of a page.
     * <b>Note: </b>all pages are assumed to have equal bounds.
     */
    Rectangle getPageBounds();

    /**
     * Returns a {@code FluidSwipeAnimationPainterDelegate} instance to which delegate the animation painting process.
     *
     * @return the specified animation painter delegate, if available; otherwise {@code null} is returned
     */
    AnimPainterDelegate getFluidSwipeAnimationPainterDelegate();

    /**
     * Convenience method
     * @return {@code true} if a painter delegate exists, and if it is ready to paint; otherwise {@code false} is returned.
     * @see AnimPainterDelegate#isActive()
     */
    default boolean swipeAnimationDelegateCanPaint() {
        final AnimPainterDelegate fluidSwipeAnimationPainterDelegate = getFluidSwipeAnimationPainterDelegate();
        return fluidSwipeAnimationPainterDelegate != null && fluidSwipeAnimationPainterDelegate.isActive();
    }
}
