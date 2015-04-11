package yanbin.imagelazyload;


import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

/**
 * ListView异步加载图片，图片错位问题解决方案
 * 本人参考http://negativeprobability.blogspot.com/2011/08/lazy-loading-of-images-in-listview.html 修改
 * 问题分析：我们在使用AsyncTask异步下载图片的时候，经常会用到convertView的重用，一般情况下，滑动后第一个可见的元素（我们给它命个名，称为A1）和
 * listview的第一个元素（A）是公用一个convertView的（一般情况），此时问题就来了，如果异步下载图片执行的比较慢，第一个元素对应的url
 * 下载的图片会放到第一个元素上面还是滑动后第一个可见的元素上面呢？
 * 解决方案：给每个listview的ImageView设置tag，使用findViewWithTag()的方式来得到ImageView，此时图片就不会产生错位了。而且
 * 值得注意的是，由于A和A1使用同一个convertView,因此如果A中的ImageView已经有值得话，当A2展示的并且它所对应的图片还未加载完成的时候，
 * A2会显示A上的图片，这明显是不对的，因此我们这里给它设置一个默认的icon，即mHolder.mImageView.setImageResource(R.drawable.ic_launcher);
 * 思路简介：一般listview中涉及到图片加载的，为了提高用户体验，一般会使用一个软引用和存文件的方式来实现。不过还见过这样来实现的：即用一个HashMap来存放很小一部分的数据，
 * 当HashMap中的大小达到一定的值时，清空HashMap，并且将数据放入软应用中。
 * 注意操作sdcard权限已经网络访问权限的加入
 * 我们这里就直接使用软引用了，然后存放到SDCard或者文件中。能力有限，代码在多线程的同步已经异常处理上还有待修正，仅供参考学习。实际应用场景中还有待优化。
 * @author yanbin
 *
 */
public class MainActivity extends Activity {
	ListView mListView;
	ImageDownloader mDownloader;
	MyListAdapter myListAdapter;
	private static final String TAG = "MainActivity";
	private static final String[] URLS = {
		"http://lh5.ggpht.com/_mrb7w4gF8Ds/TCpetKSqM1I/AAAAAAAAD2c/Qef6Gsqf12Y/s144-c/_DSC4374%20copy.jpg",
		"http://lh5.ggpht.com/_Z6tbBnE-swM/TB0CryLkiLI/AAAAAAAAVSo/n6B78hsDUz4/s144-c/_DSC3454.jpg",
		"http://lh3.ggpht.com/_GEnSvSHk4iE/TDSfmyCfn0I/AAAAAAAAF8Y/cqmhEoxbwys/s144-c/_MG_3675.jpg",
		"http://lh6.ggpht.com/_Nsxc889y6hY/TBp7jfx-cgI/AAAAAAAAHAg/Rr7jX44r2Gc/s144-c/IMGP9775a.jpg",
		"http://lh3.ggpht.com/_lLj6go_T1CQ/TCD8PW09KBI/AAAAAAAAQdc/AqmOJ7eg5ig/s144-c/Juvenile%20Gannet%20despute.jpg",
		"http://lh6.ggpht.com/_ZN5zQnkI67I/TCFFZaJHDnI/AAAAAAAABVk/YoUbDQHJRdo/s144-c/P9250508.JPG",
		"http://lh4.ggpht.com/_XjNwVI0kmW8/TCOwNtzGheI/AAAAAAAAC84/SxFJhG7Scgo/s144-c/0014.jpg",
		"http://lh6.ggpht.com/_lnDTHoDrJ_Y/TBvKsJ9qHtI/AAAAAAAAG6g/Zll2zGvrm9c/s144-c/000007.JPG",
		"http://lh6.ggpht.com/_qvCl2efjxy0/TCIVI-TkuGI/AAAAAAAAOUY/vbk9MURsv48/s144-c/DSC_0844.JPG",
		"http://lh4.ggpht.com/_4f1e_yo-zMQ/TCe5h9yN-TI/AAAAAAAAXqs/8X2fIjtKjmw/s144-c/IMG_1786.JPG",
		"http://lh5.ggpht.com/_hepKlJWopDg/TB-_WXikaYI/AAAAAAAAElI/715k4NvBM4w/s144-c/IMG_0075.JPG",
		"http://lh6.ggpht.com/_ZGv_0FWPbTE/TB-_GLhqYBI/AAAAAAABVxs/cVEvQzt0ke4/s144-c/IMG_1288_hf.jpg",
		"http://lh6.ggpht.com/_a29lGRJwo0E/TBqOK_tUKmI/AAAAAAAAVbw/UloKpjsKP3c/s144-c/31012332.jpg",
		"http://lh3.ggpht.com/_55Lla4_ARA4/TB6xbyxxJ9I/AAAAAAABTWo/GKe24SwECns/s144-c/Bluebird%20049.JPG",
		"http://lh3.ggpht.com/_iVnqmIBYi4Y/TCaOH6rRl1I/AAAAAAAA1qg/qeMerYQ6DYo/s144-c/Kwiat_100626_0016.jpg",
		"http://lh6.ggpht.com/_QFsB_q7HFlo/TCItd_2oBkI/AAAAAAAAFsk/4lgJWweJ5N8/s144-c/3705226938_d6d66d6068_o.jpg",
		"http://lh6.ggpht.com/_loGyjar4MMI/S-InVNkTR_I/AAAAAAAADJY/Fb5ifFNGD70/s144-c/Moving%20Rock.jpg",
		"http://lh4.ggpht.com/_L7i4Tra_XRY/TBtxjScXLqI/AAAAAAAAE5o/ue15HuP8eWw/s144-c/opera%20house%20II.jpg",
		"http://lh3.ggpht.com/_rfAz5DWHZYs/S9cstBTv1iI/AAAAAAAAeYA/EyZPUeLMQ98/s144-c/DSC_6425.jpg",
		"http://lh6.ggpht.com/_iGI-XCxGLew/S-iYQWBEG-I/AAAAAAAACB8/JuFti4elptE/s144-c/norvig-polar-bear.jpg",
		"http://lh3.ggpht.com/_M3slUPpIgmk/SlbnavqG1cI/AAAAAAAACvo/z6-CnXGma7E/s144-c/mf_003.jpg",
		"http://lh4.ggpht.com/_loGyjar4MMI/S-InQvd_3hI/AAAAAAAADIw/dHvCFWfyHxQ/s144-c/Rainbokeh.jpg",
		"http://lh5.ggpht.com/_6_dLVKawGJA/SMwq86HlAqI/AAAAAAAAG5U/q1gDNkmE5hI/s144-c/mobius-glow.jpg",
		"http://lh3.ggpht.com/_QFsB_q7HFlo/TCItc19Jw3I/AAAAAAAAFs4/nPfiz5VGENk/s144-c/4551649039_852be0a952_o.jpg",
		"http://lh6.ggpht.com/_TQY-Nm7P7Jc/TBpjA0ks2MI/AAAAAAAABcI/J6ViH98_poM/s144-c/IMG_6517.jpg",
		"http://lh3.ggpht.com/_rfAz5DWHZYs/S9cLAeKuueI/AAAAAAAAeYU/E19G8DOlJRo/s144-c/DSC_4397_8_9_tonemapped2.jpg",
		"http://lh4.ggpht.com/_TQY-Nm7P7Jc/TBpi6rKfFII/AAAAAAAABbg/79FOc0Dbq0c/s144-c/david_lee_sakura.jpg",
		"http://lh3.ggpht.com/_TQY-Nm7P7Jc/TBpi8EJ4eDI/AAAAAAAABb0/AZ8Cw1GCaIs/s144-c/Hokkaido%20Swans.jpg",
		"http://lh3.ggpht.com/_1aZMSFkxSJI/TCIjB6od89I/AAAAAAAACHM/CLWrkH0ziII/s144-c/079.jpg",
		"http://lh5.ggpht.com/_loGyjar4MMI/S-InWuHkR9I/AAAAAAAADJE/wD-XdmF7yUQ/s144-c/Colorado%20River%20Sunset.jpg",
		"http://lh3.ggpht.com/_0YSlK3HfZDQ/TCExCG1Zc3I/AAAAAAAAX1w/9oCH47V6uIQ/s144-c/3138923889_a7fa89cf94_o.jpg",
		"http://lh6.ggpht.com/_K29ox9DWiaM/TAXe4Fi0xTI/AAAAAAAAVIY/zZA2Qqt2HG0/s144-c/IMG_7100.JPG",
		"http://lh6.ggpht.com/_0YSlK3HfZDQ/TCEx16nJqpI/AAAAAAAAX1c/R5Vkzb8l7yo/s144-c/4235400281_34d87a1e0a_o.jpg",
		"http://lh4.ggpht.com/_8zSk3OGcpP4/TBsOVXXnkTI/AAAAAAAAAEo/0AwEmuqvboo/s144-c/yosemite_forrest.jpg",
		"http://lh4.ggpht.com/_6_dLVKawGJA/SLZToqXXVrI/AAAAAAAAG5k/7fPSz_ldN9w/s144-c/coastal-1.jpg",
		"http://lh4.ggpht.com/_WW8gsdKXVXI/TBpVr9i6BxI/AAAAAAABhNg/KC8aAJ0wVyk/s144-c/IMG_6233_1_2-2.jpg",
		"http://lh3.ggpht.com/_loGyjar4MMI/S-InS0tJJSI/AAAAAAAADHU/E8GQJ_qII58/s144-c/Windmills.jpg",
		"http://lh4.ggpht.com/_loGyjar4MMI/S-InbXaME3I/AAAAAAAADHo/4gNYkbxemFM/s144-c/Frantic.jpg",
		"http://lh5.ggpht.com/_loGyjar4MMI/S-InKAviXzI/AAAAAAAADHA/NkyP5Gge8eQ/s144-c/Rice%20Fields.jpg",
		"http://lh3.ggpht.com/_loGyjar4MMI/S-InZA8YsZI/AAAAAAAADH8/csssVxalPcc/s144-c/Seahorse.jpg",
		"http://lh3.ggpht.com/_syQa1hJRWGY/TBwkCHcq6aI/AAAAAAABBEg/R5KU1WWq59E/s144-c/Antelope.JPG",
		"http://lh5.ggpht.com/_MoEPoevCLZc/S9fHzNgdKDI/AAAAAAAADwE/UAno6j5StAs/s144-c/c84_7083.jpg",
		"http://lh4.ggpht.com/_DJGvVWd7IEc/TBpRsGjdAyI/AAAAAAAAFNw/rdvyRDgUD8A/s144-c/Free.jpg",
		"http://lh6.ggpht.com/_iO97DXC99NY/TBwq3_kmp9I/AAAAAAABcz0/apq1ffo_MZo/s144-c/IMG_0682_cp.jpg",
	"http://lh4.ggpht.com/_7V85eCJY_fg/TBpXudG4_PI/AAAAAAAAPEE/8cHJ7G84TkM/s144-c/20100530_120257_0273-Edit-2.jpg" };

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		mListView = (ListView) findViewById(R.id.listview);
		myListAdapter = new MyListAdapter();
		mListView.setAdapter(myListAdapter);
	}

	private class MyListAdapter extends BaseAdapter {
		private ViewHolder mHolder;

		@Override
		public int getCount() {
			return URLS.length;
		}

		@Override
		public Object getItem(int position) {
			return URLS[position];
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			System.out.println("position==" + position + "converview=="
					+ convertView);
			Log.i(TAG, "position==" + position + ",converview==" + convertView);
			if (convertView == null) {
				convertView = getLayoutInflater().inflate(R.layout.single_data,
						null);
				mHolder = new ViewHolder();
				mHolder.mImageView = (ImageView) convertView.findViewById(R.id.image_view);
				mHolder.mTextView = (TextView) convertView.findViewById(R.id.text_view);
				convertView.setTag(mHolder);
			}else {
				mHolder = (ViewHolder) convertView.getTag();
			}
			final String url = URLS[position];
			mHolder.mTextView.setText(URLS[position]);
			mHolder.mImageView.setTag(URLS[position]);
			//			final TextView tv = (TextView) convertView
			//					.findViewById(R.id.text_view);
			//			final ImageView iv = (ImageView) convertView
			//					.findViewById(R.id.image_view);
			//			iv.setTag(url);
			//			tv.setText(url != null ? url.substring(url.lastIndexOf("/") + 1)
			//					: "");
			if (mDownloader == null) {
				mDownloader = new ImageDownloader();
			}
			//这句代码的作用是为了解决convertView被重用的时候，图片预设的问题
			mHolder.mImageView.setImageResource(R.drawable.ic_launcher);
			if (mDownloader != null) {
				mDownloader.imageDownload(url, mHolder.mImageView, "/yanbin",MainActivity.this, new OnImageDownload() {

					@Override
					public void onDownloadSucc(Bitmap bitmap,
							String c_url) {
						ImageView imageView = (ImageView) mListView.findViewWithTag(c_url);
						if (imageView != null && c_url.equals(imageView.getTag())) {
							imageView.setImageBitmap(bitmap);
							imageView.setTag("");
						} else if (imageView != null) {
							imageView.setImageResource(R.drawable.ic_launcher);
						}
					}
				});
			}
			return convertView;

		}

		/**
		 * 使用ViewHolder来优化listview
		 * @author yanbin
		 *
		 */
		private class ViewHolder {
			ImageView mImageView;
			TextView mTextView;
		}
	}
}