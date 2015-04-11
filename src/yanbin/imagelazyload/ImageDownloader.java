package yanbin.imagelazyload;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.widget.ImageView;

/**
 * 图片异步下载类，包括图片的软应用缓存已经将图片存放到SDCard或者文件中
 * @author yanbin
 *
 */
public class ImageDownloader {
	private Map<String, SoftReference<Bitmap>> imageCaches = new HashMap<String, SoftReference<Bitmap>>();
	public void imageDownload(String url,ImageView mImageView,
			String path,Activity mActivity,OnImageDownload download){
		SoftReference<Bitmap> currBitmap = imageCaches.get(url);
		Bitmap softRefBitmap = null;
		if(currBitmap != null){
			softRefBitmap = currBitmap.get();
		}
		String imageName = "";
		if(url != null){
			imageName = Util.getInstance().getImageName(url);
		}
		Bitmap bitmap = getBitmapFromFile(mActivity,imageName,path);
		//先从软引用中拿数据
		if(currBitmap != null && mImageView != null && softRefBitmap != null && url.equals(mImageView.getTag())){
			mImageView.setImageBitmap(softRefBitmap);
		}
		//软引用中没有，从文件中拿数据
		else if(bitmap != null && mImageView != null && url.equals(mImageView.getTag())){
			mImageView.setImageBitmap(bitmap);
		}
		//文件中也没有，异步下载图片
		else{
			new MyAsyncTask(url, mImageView, path,mActivity,download).execute();
		}
	}
	
	/**
	 * 从文件中拿图片
	 * @param mActivity 
	 * @param imageName 图片名字
	 * @param path 图片路径
	 * @return
	 */
	private Bitmap getBitmapFromFile(Activity mActivity,String imageName,String path){
		Bitmap bitmap = null;
		if(imageName != null){
			File file = null;
			String real_path = "";
			try {
				if(Util.getInstance().hasSDCard()){
					real_path = Util.getInstance().getExtPath() + (path != null && path.startsWith("/") ? path : "/" + path);
				}else{
					real_path = Util.getInstance().getPackagePath(mActivity) + (path != null && path.startsWith("/") ? path : "/" + path);
				}
				file = new File(real_path, imageName);
				if(file.exists())
				bitmap = BitmapFactory.decodeStream(new FileInputStream(file));
			} catch (Exception e) {
				e.printStackTrace();
				bitmap = null;
			}
		}
		return bitmap;
	}
	
	/**
	 * 将下载好的图片存放到文件中
	 * @param path 图片路径
	 * @param mActivity
	 * @param imageName 图片名字
	 * @param bitmap 图片
	 * @return
	 */
	private boolean setBitmapToFile(String path,Activity mActivity,String imageName,Bitmap bitmap){
		File file = null;
		String real_path = "";
		try {
			if(Util.getInstance().hasSDCard()){
				real_path = Util.getInstance().getExtPath() + (path != null && path.startsWith("/") ? path : "/" + path);
			}else{
				real_path = Util.getInstance().getPackagePath(mActivity) + (path != null && path.startsWith("/") ? path : "/" + path);
			}
			file = new File(real_path, imageName);
			if(!file.exists()){
				File file2 = new File(real_path + "/");
				file2.mkdirs();
			}
			file.createNewFile();
			FileOutputStream fos = null;
			if(Util.getInstance().hasSDCard()){
				fos = new FileOutputStream(file);
			}else{
				fos = mActivity.openFileOutput(imageName, Context.MODE_PRIVATE);
			}
			
			if (imageName != null && (imageName.contains(".png") || imageName.contains(".PNG"))){
				bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos);
			}
			else{
				bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
			}
			fos.flush();
			if(fos != null){
				fos.close();
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	private void removeBitmapFromFile(String path,Activity mActivity,String imageName){
		File file = null;
		String real_path = "";
		try {
			if(Util.getInstance().hasSDCard()){
				real_path = Util.getInstance().getExtPath() + (path != null && path.startsWith("/") ? path : "/" + path);
			}else{
				real_path = Util.getInstance().getPackagePath(mActivity) + (path != null && path.startsWith("/") ? path : "/" + path);
			}
			file = new File(real_path, imageName);
			if(file != null)
			file.delete();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	private class MyAsyncTask extends AsyncTask<String, Void, Bitmap>{
		private ImageView mImageView;
		private String url;
		private OnImageDownload download;
		private String path;
		private Activity mActivity;
		
		public MyAsyncTask(String url,ImageView mImageView,String path,Activity mActivity,OnImageDownload download){
			this.mImageView = mImageView;
			this.url = url;
			this.path = path;
			this.mActivity = mActivity;
			this.download = download;
		}

		@Override
		protected Bitmap doInBackground(String... params) {
			Bitmap data = null;
			if(url != null){
				try {
					URL c_url = new URL(url);
					InputStream bitmap_data = c_url.openStream();
					data = BitmapFactory.decodeStream(bitmap_data);
					String imageName = Util.getInstance().getImageName(url);
					if(!setBitmapToFile(path,mActivity,imageName, data)){
						removeBitmapFromFile(path,mActivity,imageName);
					}
					imageCaches.put(url, new SoftReference<Bitmap>(data.createScaledBitmap(data, 100, 100, true)));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			return data;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			//缓存图片并且回调设置图片
			if(download != null){
				download.onDownloadSucc(result,url);
			}
			super.onPostExecute(result);
		}
		
	}
}
