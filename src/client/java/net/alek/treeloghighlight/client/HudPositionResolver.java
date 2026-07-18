package net.alek.treeloghighlight.client;

import net.alek.treeloghighlight.client.TreeLogHighlightConfig.HudAnchor;

public class HudPositionResolver {

    public static int resolveX(HudAnchor anchor, int xOffset, int elementWidth, int screenWidth) {
        return switch (anchor) {
            case TOP_LEFT, MIDDLE_LEFT, BOTTOM_LEFT -> xOffset;
            case TOP_CENTER, CENTER, BOTTOM_CENTER -> (screenWidth - elementWidth) / 2 + xOffset;
            case TOP_RIGHT, MIDDLE_RIGHT, BOTTOM_RIGHT -> screenWidth - elementWidth - xOffset;
        };
    }

    public static int resolveY(HudAnchor anchor, int yOffset, int elementHeight, int screenHeight) {
        return switch (anchor) {
            case TOP_LEFT, TOP_CENTER, TOP_RIGHT -> yOffset;
            case MIDDLE_LEFT, CENTER, MIDDLE_RIGHT -> (screenHeight - elementHeight) / 2 + yOffset;
            case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT -> screenHeight - elementHeight - yOffset;
        };
    }

    public static TreeLogHighlightConfig.HudPos fromDropPoint(
            int dropX, int dropY, int elementWidth, int elementHeight,
            int screenWidth, int screenHeight) {

        boolean left = dropX < screenWidth / 3;
        boolean right = dropX > screenWidth * 2 / 3;
        boolean top = dropY < screenHeight / 3;
        boolean bottom = dropY > screenHeight * 2 / 3;

        HudAnchor anchor;
        if (top && left) anchor = HudAnchor.TOP_LEFT;
        else if (top && right) anchor = HudAnchor.TOP_RIGHT;
        else if (top) anchor = HudAnchor.TOP_CENTER;
        else if (bottom && left) anchor = HudAnchor.BOTTOM_LEFT;
        else if (bottom && right) anchor = HudAnchor.BOTTOM_RIGHT;
        else if (bottom) anchor = HudAnchor.BOTTOM_CENTER;
        else if (left) anchor = HudAnchor.MIDDLE_LEFT;
        else if (right) anchor = HudAnchor.MIDDLE_RIGHT;
        else anchor = HudAnchor.CENTER;

        int xOffset = switch (anchor) {
            case TOP_LEFT, MIDDLE_LEFT, BOTTOM_LEFT -> dropX;
            case TOP_CENTER, CENTER, BOTTOM_CENTER -> dropX - (screenWidth - elementWidth) / 2;
            case TOP_RIGHT, MIDDLE_RIGHT, BOTTOM_RIGHT -> screenWidth - elementWidth - dropX;
        };

        int yOffset = switch (anchor) {
            case TOP_LEFT, TOP_CENTER, TOP_RIGHT -> dropY;
            case MIDDLE_LEFT, CENTER, MIDDLE_RIGHT -> dropY - (screenHeight - elementHeight) / 2;
            case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT -> screenHeight - elementHeight - dropY;
        };

        return new TreeLogHighlightConfig.HudPos(anchor, xOffset, yOffset);
    }
}