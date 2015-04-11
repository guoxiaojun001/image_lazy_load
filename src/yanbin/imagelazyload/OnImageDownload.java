package yanbin.imagelazyload;

import android.graphics.Bitmap;

/**
 * 图片异步下载完成后回调
 * @author yanbin
 *
 */
public interface OnImageDownload {
	void onDownloadSucc(Bitmap bitmap,String c_url);
}
