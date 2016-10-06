package jp.co.disney.apps.dm.disneyshare;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.widget.ImageView;

public class MenuItemImageView extends ImageView {

    public MenuItemImageView(Context context) {
        this(context, null);
    }

    public MenuItemImageView(Context context, AttributeSet attributeset) {
        this(context, attributeset, 0);
    }

    public MenuItemImageView(Context context, AttributeSet attributeset, int i) {
        super(context, attributeset, i);
        setClickable(true);
    }

    protected void drawableStateChanged() {
        super.drawableStateChanged();

        if (isPressed()) {
            getDrawable().setColorFilter(Color.rgb(13, 162, 221), PorterDuff.Mode.SRC_ATOP);

        } else {
            getDrawable().clearColorFilter();
        }
    }
}
