package com.example.audiorecord;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

public class AudioRecored extends Activity {
	private Button mAudioStartBtn;
	private Button mAudioStopBtn;
	private File mRecAudioFile; // 录制的音频文件
	private File mRecAudioPath; // 录制的音频文件路徑
	private MediaRecorder mMediaRecorder;// MediaRecorder对象
	private String strTempFile = "recaudio_";// 零时文件的前缀
	private ListView mList;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_audio_recored);
		initRecAudioPath();
		initList();
		initButton();
	}

	private void initList() {
		mList = (ListView) findViewById(R.id.simplelist);
		setListEmptyView();
		setOnItemClickListener();
		setOnItemLongClickListener();
		musicList();
	}

	private void initButton() {
		mAudioStartBtn = (Button) findViewById(R.id.mediarecorder1_AudioStartBtn);
		mAudioStopBtn = (Button) findViewById(R.id.mediarecorder1_AudioStopBtn);

		/* 开始按钮事件监听 */
		mAudioStartBtn.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				/* 按钮状态 */
				mAudioStartBtn.setEnabled(false);
				mAudioStopBtn.setEnabled(true);
				mHandler.sendEmptyMessage(MSG_RECORD);
			}
		});
		/* 停止按钮事件监听 */
		mAudioStopBtn.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				/* 按钮状态 */
				mAudioStartBtn.setEnabled(true);
				mAudioStopBtn.setEnabled(false);
				mHandler.sendEmptyMessage(MSG_STOP);
			}
		});

		/* 按钮状态 */
		mAudioStartBtn.setEnabled(true);
		mAudioStopBtn.setEnabled(false);
	}

	private void startRecord() {
		try {
			if (!initRecAudioPath()) {
				return;
			}
			/* 按钮状态 */
			mAudioStartBtn.setEnabled(false);
			mAudioStopBtn.setEnabled(true);
			
			/* ①Initial：实例化MediaRecorder对象 */
			mMediaRecorder = new MediaRecorder();
			/* ②setAudioSource/setVedioSource */
			mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);// 设置麦克风
			/*
			 * ②设置输出文件的格式：THREE_GPP/MPEG-4/RAW_AMR/Default
			 * THREE_GPP(3gp格式，H263视频
			 * /ARM音频编码)、MPEG-4、RAW_AMR(只支持音频且音频编码要求为AMR_NB)
			 */
			mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
			/* ②设置音频文件的编码：AAC/AMR_NB/AMR_MB/Default */
			mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
			/* ②设置输出文件的路径 */
			try {
				mRecAudioFile = File.createTempFile(strTempFile, ".amr",
						mRecAudioPath);

			} catch (Exception e) {
				e.printStackTrace();
			}
			mMediaRecorder.setOutputFile(mRecAudioFile.getAbsolutePath());
			/* ③准备 */
			mMediaRecorder.prepare();
			/* ④开始 */
			mMediaRecorder.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void stopRecord() {
		if (mRecAudioFile != null) {
			/* 按钮状态 */
			mAudioStartBtn.setEnabled(true);
			mAudioStopBtn.setEnabled(false);
			/* ⑤停止录音 */
			mMediaRecorder.stop();
			/* 将录音文件添加到List中 */
			addItem(mRecAudioFile.getName());
			/* ⑥释放MediaRecorder */
			mMediaRecorder.release();
			mMediaRecorder = null;
		}
	}

	private static final int MSG_RECORD = 0;
	private static final int MSG_STOP = 1;
	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_RECORD:
				startRecord();
				break;
			case MSG_STOP:
				stopRecord();
				break;
			default:
				break;
			}
		};
	};

	/* 播放录音文件 */
	private void playMusic(File file) {
		Intent intent = new Intent();
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.setAction(android.content.Intent.ACTION_VIEW);
		/* 设置文件类型 */
		intent.setDataAndType(Uri.fromFile(file), "audio");
		startActivity(intent);
	}

	/* 播放列表 */
	public void musicList() {
		List<Map<String, Object>> listdata = getData();
		setListAdapter(listdata);
		mList.setTag(listdata);
	}

	private void addItem(String item) {
		List<Map<String, Object>> listdata = (List<Map<String, Object>>) mList
				.getTag();
		int count = listdata.size();
		listdata.add(getOneItem(item));
		setListAdapter(listdata);
	}

	public void deleteItem(int position) {
		List<Map<String, Object>> listdata = (List<Map<String, Object>>) mList
				.getTag();
		listdata.remove(position);
		setListAdapter(listdata);
	}

	private void setListAdapter(List<Map<String, Object>> listdata) {
		SimpleAdapter adapter = new SimpleAdapter(getBaseContext(), listdata,
				R.layout.simple_item, new String[] { "text" },
				new int[] { R.id.text });
		mList.setAdapter(adapter);
	}

	private MusicFilter mFilter = new MusicFilter();

	private List<Map<String, Object>> getData() {
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		File home = mRecAudioPath;
		if (home != null) {
			File[] files = home.listFiles(mFilter);
			if (files != null && files.length > 0) {
				for (File file : files) {
					list.add(getOneItem(file.getName()));
				}
			}
		}
		return list;
	}

	private Map<String, Object> getOneItem(String text) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("text", text);
		return map;
	}

	private void setListEmptyView() {
		/*
		 * TextView emptyView = new TextView(getBaseContext());
		 * emptyView.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
		 * LayoutParams.FILL_PARENT));
		 * emptyView.setText("This appears when the list is empty");
		 * emptyView.setVisibility(View.GONE);
		 * ((ViewGroup)mList.getParent()).addView(emptyView);
		 * mList.setEmptyView(emptyView);
		 */
		View emptyView = findViewById(R.id.empty);
		mList.setEmptyView(emptyView);
	}

	private void setOnItemClickListener() {
		mList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> l, View v, int position,
					long id) {
				List<Map<String, Object>> listdata = (List<Map<String, Object>>) mList
						.getTag();
				Map<String, Object> map = listdata.get(position);
				String name = (String) map.get("text");
				/* 得到被点击的文件 */
				File playfile = new File(mRecAudioPath.getAbsolutePath()
						+ File.separator + name);
				/* 播放 */
				playMusic(playfile);
			}
		});
	}

	private void setOnItemLongClickListener() {
		mList.setOnItemLongClickListener(new OnItemLongClickListener() {
			String mName = null;
			int mPosition = 0;

			@Override
			public boolean onItemLongClick(AdapterView<?> l, View v,
					int position, long id) {
				List<Map<String, Object>> listdata = (List<Map<String, Object>>) l
						.getTag();
				Map<String, Object> map = listdata.get(position);

				mName = (String) map.get("text");
				mPosition = position;

				new AlertDialog.Builder(AudioRecored.this)
						.setIcon(android.R.drawable.ic_dialog_alert)
						.setTitle("小心！").setMessage("确定删除\"" + mName + "\"吗?")
						.setNegativeButton("确定", new OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int whichButton) {
								new File(mRecAudioPath.getAbsolutePath()
										+ File.separator + mName).delete();
								deleteItem(mPosition);
							}
						}).setPositiveButton("取消", new OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int whichButton) {
							}
						}).show();
				return true;
			}
		});
	}

	private boolean sdcardIsValid() {
		if (Environment.getExternalStorageState().equals(
				android.os.Environment.MEDIA_MOUNTED)) {
			return true;
		} else {
			Toast.makeText(getBaseContext(), "没有SD卡", Toast.LENGTH_LONG).show();
		}
		return false;
	}

	private boolean initRecAudioPath() {
		if (sdcardIsValid()) {
			String path = Environment.getExternalStorageDirectory().toString()
					+ File.separator + "record";// 得到SD卡得路径
			mRecAudioPath = new File(path);
			if (!mRecAudioPath.exists()) {
				mRecAudioPath.mkdirs();
			}
		} else {
			mRecAudioPath = null;
		}
		return mRecAudioPath != null;
	}
}

/* 过滤文件类型 */
class MusicFilter implements FilenameFilter {
	public boolean accept(File dir, String name) {
		return (name.endsWith(".amr"));
	}
}
