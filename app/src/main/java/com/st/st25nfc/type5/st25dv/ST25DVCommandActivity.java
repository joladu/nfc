/*
  * @author STMicroelectronics MMY Application team
  *
  ******************************************************************************
  * @attention
  *
  * <h2><center>&copy; COPYRIGHT 2017 STMicroelectronics</center></h2>
  *
  * Licensed under ST MIX_MYLIBERTY SOFTWARE LICENSE AGREEMENT (the "License");
  * You may not use this file except in compliance with the License.
  * You may obtain a copy of the License at:
  *
  *        http://www.st.com/Mix_MyLiberty
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied,
  * AND SPECIFICALLY DISCLAIMING THE IMPLIED WARRANTIES OF MERCHANTABILITY,
  * FITNESS FOR A PARTICULAR PURPOSE, AND NON-INFRINGEMENT.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  *
  ******************************************************************************
*/

package com.st.st25nfc.type5.st25dv;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.st.st25nfc.R;
import com.st.st25nfc.generic.ST25Menu;
import com.st.st25nfc.generic.STFragment;
import com.st.st25nfc.generic.STFragmentActivity;
import com.st.st25nfc.generic.STPagerAdapter;
import com.st.st25nfc.generic.SlidingTabLayout;
import com.st.st25nfc.generic.WriteFragmentActivity;
import com.st.st25nfc.generic.util.UIHelper.STFragmentId;
import com.st.st25sdk.STException;
import com.st.st25sdk.type5.st25dv.ST25DVTag;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class ST25DVCommandActivity extends STFragmentActivity
        implements NavigationView.OnNavigationItemSelectedListener, STFragment.STFragmentListener, View.OnClickListener{

    final static String TAG = "ST25DVCommandActivity";

    // Set here the Toolbar to use for this activity
    private int toolbar_res = R.menu.toolbar_empty;


    public ST25DVTag mST25DVTag;


    private SlidingTabLayout mSlidingTabLayout;

    private EditText mCommandEt;
    private ListView mCommandHistoryLv;
    private CommandsListViewAdapter mCommandListAdapter;
    /**
     * 线程池 复用线程池
     */
    private ExecutorService executorService;
    /**
     * 命令写完后，需等待一定时间再读取，才能读到表回复的数据
     */
    private PopCommandGridView mPopCommandGridView;
    private String[] mCommandGridArr;

//    当前命令名称
    private String mCurCommandName;

    private AlertDialog alertDialog;

//    private boolean mIsCommonCommand = true;

    private int frameNo = 1;
    private byte[] mSendDataByteArr;

    private List<String> mAddrHistList = new ArrayList<>();
    private List<String> mSecondList = new ArrayList<>();

    private List<CommondSendResponseEntity> mDataList = new ArrayList<>();


    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_command_st25dv);

        if (super.getTag() instanceof ST25DVTag) {
            mST25DVTag = (ST25DVTag) super.getTag();
        }
        if (mST25DVTag == null) {
            showToast(R.string.invalid_tag);
            goBackToMainActivity();
            return;
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.title_command_build);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        mMenu = ST25Menu.newInstance(super.getTag());
        NavigationView navigationView = (NavigationView) findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(this);
        mMenu.inflateMenu(navigationView);

        mCommandEt = (EditText) findViewById(R.id.edit_send);
        mCommandHistoryLv = (ListView) findViewById(R.id.msg_communication_lv);

        mCommandListAdapter = new CommandsListViewAdapter();

        mCommandGridArr = this.getResources().getStringArray(R.array.grid_command);
//        创建线程池 建立工作区
        executorService = Executors.newFixedThreadPool(5);

    }


    /**
     * PopupWindow中的点击事件传递到此解析
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()){
//            case R.id.common_command_tv:
//                if (!mIsCommonCommand){
//                    if (null != mPopCommandGridView){
//                        mPopCommandGridView.setGridViewAdapter(mCommonCommandArr,true);
//                        mIsCommonCommand = true;
//                    }
//                }
//                break;
//            case R.id.expand_command_tv:
//                if (mIsCommonCommand){
//                    if (null != mPopCommandGridView){
//                        mPopCommandGridView.setGridViewAdapter(mExpandCommandArr,false);
//                        mIsCommonCommand = false;
//                    }
//                }
//                break;
            case R.id.first_scan_iv:
//                startActivityForResult(new Intent(ST25DVCommandActivity.this, SimpleScannerActivity.class), Request_Bar_Code_First);
                break;
            case R.id.first_pulldown_iv:
//                展示历史水表的对话框列表
                if (null != mPopCommandGridView){
                    mCurCommandName = mPopCommandGridView.getTitleByIndex(1);
                }
                showListDialog(mAddrHistList.toArray(new String[0]), 1);
                break;
            case R.id.close_popup_iv:
                if (null != mPopCommandGridView) {
                    mPopCommandGridView.dismiss();
                }
                break;
            case R.id.cancel_btn:
                if (null != mPopCommandGridView) {
                    mPopCommandGridView.hindeOrShowInputLl(true);
                }
                break;
            case R.id.confirm_btn:
                confirmBuildCommand(v);
                break;
        }
    }

    /**
     * 点击“构建命令”弹出命令选择框
     */
    public void popupCommandView(View view) {
        mPopCommandGridView = new PopCommandGridView(this, new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                点击相应 根据命令字来生成相应的命令帧
                clickGridView(position);
            }
        }, this,mCommandGridArr);
        mPopCommandGridView.showAtLocation(this.findViewById(R.id.drawer), Gravity.CENTER | Gravity.CENTER_HORIZONTAL,0,0);
    }

    /**
     * 展示下拉框
     */
    public void showListDialog(final String[] selectArr, final int spannerIndex) {
        if (null == selectArr || selectArr.length == 0) {
            Toast.makeText(this, "无可选内容", Toast.LENGTH_SHORT).show();
            return;
        }
        String title = mCurCommandName;
        if (null != mPopCommandGridView) {
            title = "请选择" + mPopCommandGridView.getTitleByIndex(spannerIndex);
        }
        final AlertDialog.Builder listDialog =
                new AlertDialog.Builder(this);
        listDialog.setTitle(title);
        listDialog.setItems(selectArr, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (null != mPopCommandGridView) {
                    Toast.makeText(ST25DVCommandActivity.this, selectArr[which], Toast.LENGTH_SHORT).show();
                    switch (mCurCommandName) {
                        case "读历史累积量":
                            if (spannerIndex == 1) {
                                mPopCommandGridView.setEtContentByIndex((selectArr[which]), spannerIndex);
                            } else {
                                mPopCommandGridView.setEtContentByIndex(getValueByMonth(selectArr[which]), spannerIndex);
                            }
                            break;
                        case "读历史冻结量":
                            if (spannerIndex == 1) {
                                mPopCommandGridView.setEtContentByIndex((selectArr[which]), spannerIndex);
                            } else {
                                mPopCommandGridView.setEtContentByIndex(getValueByDay(selectArr[which]), spannerIndex);
                            }
                            break;
                        case "设置无线信道":
                            if (spannerIndex == 1) {
                                mPopCommandGridView.setEtContentByIndex((selectArr[which]), spannerIndex);
                            } else {
                                mPopCommandGridView.setEtContentByIndex(getRateHexStrByPos(which), spannerIndex);
                            }
                            break;
                        default:
                            mPopCommandGridView.setEtContentByIndex(selectArr[which], spannerIndex);
                    }
                }
            }
        });
        listDialog.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (null != alertDialog) {
                    alertDialog.dismiss();
                }
            }
        });
        alertDialog = listDialog.show();
    }



    /**
     * 发送按钮
     */
    public void sendBtn(View view) {
        sendCommand();
    }

    /**
     * 发送命令
     */
    public void sendCommand(){
        executorService.submit(new WriteCommandRunnable());
    }

    /**
     * 发送命令线程
     */
    class WriteCommandRunnable implements Runnable {
        public void run() {
            byte buffer[] = null;

            try {
//                getTag().writeBytes(0, new byte[]{(byte) 1,(byte) 1,(byte) 1,(byte) 1,(byte) 1});
                getTag().writeBytes(0, mSendDataByteArr);

                buffer = getTag().readBytes(0, 64);

                // Warning: readBytes() may return less bytes than requested
                if(buffer.length != 64) {
                    showToast(R.string.error_during_read_operation, buffer.length);
                }
            } catch (STException e) {
                if (e.getMessage() != null) {
                    Log.e(TAG, e.getMessage());
                } else {
                    Log.e(TAG, "Command failed");
                }
                showToastShort(mCurCommandName+getString(R.string.Command_failed));
            }

            if (buffer != null) {
                String hexStr = HexBytesUtils.byteArrToHexStr(buffer);
                Log.e(TAG, "寄存器内容："+hexStr);
                showToastShort(mCurCommandName+getString(R.string.Command_success));
                frameNo ++;
            }
        }
    }


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(toolbar_res, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    void processIntent(Intent intent) {
        Log.d(TAG, "Process Intent");
    }

    @Override
    public void onNewIntent(Intent intent) {
        // onResume gets called after this to handle the intent
        setIntent(intent);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        return mMenu.selectItem(this, item);
    }

    public ST25DVTag getTag() {
        return mST25DVTag;
    }


    /**
     * gridview 命令选择后响应
     */
    private void clickGridView(int position) {
//        根据点击的内容，结合文档解析，生成命令帧，发送给操控器
        mCurCommandName = mCommandGridArr[position];
        switch (mCurCommandName) {
            case "读表地址":
                Protocol188Entity entity = new Protocol188Entity();
                entity.setEquipType("10");
                entity.setAddress("AAAAAAAAAAAAAA");
                entity.setControlCode("03");
                entity.setDataLen("03");
                entity.setDataFlag("0A81");
                entity.setFrameNo(formatFrameNo(frameNo));
                entity.setDataStr("");
                mSendDataByteArr = entity.rebuildHex2ByteArr();
               sendCommand();
                if (null != mPopCommandGridView) {
                    mPopCommandGridView.dismiss();
                }
                break;
            case "写表地址":
//                输入：旧地址、新地址
                secondHandle(mCurCommandName, "表旧地址", "新地址");
                break;
            case "读计量数据":
//                输入：表地址
                secondHandle(mCurCommandName, "表地址");
                break;
            case "读历史月累积量":
//                输入：表地址 、上1-18个月
                secondHandle(mCurCommandName, "表地址", "历史月（1-18）");
                for (int i = 1; i < 19; i++) {
                    mSecondList.add(i + "月");
                }
                break;
            case "读历史日冻结量":
//                输入：表地址、上1-31日累计
                secondHandle(mCurCommandName, "表地址", "历史日（1-31）");
                for (int i = 1; i < 32; i++) {
                    mSecondList.add(i + "日");
                }
                break;
            case "写机电同步数据":
//                输入：表地址、XXXXXX.XX（当前累计流量）2C
                secondHandle(mCurCommandName, "表地址", "当前累计流量(.xx)");
                break;
            case "校时":
//                输入：表地址、YYYYMMDDhhmmss
//                secondHandle(mCurCommandName,"表地址","手机时间");
                secondHandle(mCurCommandName, "表地址");//直接将手机时间设置进去
                break;
            case "出厂启用":
//                输入：表地址
                secondHandle(mCurCommandName, "表地址");
                break;
            case "设置厂内":
//                输入：表地址
                secondHandle(mCurCommandName, "表地址");
                break;
            case "读EEPROM":
//                输入：表地址、addr(2字节)、len（1字节）
                secondHandle(mCurCommandName, "表地址", "ADDR地址(四位数字)", "LEN长度(<=64)");
                break;
            case "读电池电压":
//                输入：表地址
                secondHandle(mCurCommandName, "表地址");
                break;
            case "清表数据":
//                输入：表地址
                secondHandle(mCurCommandName, "表地址");
                break;
            case "读软件版本号":
//                输入：表地址
                secondHandle(mCurCommandName, "表地址");
                break;
            case "读表运行数据":
//                输入：表地址
                secondHandle(mCurCommandName, "表地址");
                break;
            case "读Boot版本号":
//                输入：表地址
                secondHandle(mCurCommandName, "表地址");
                break;
//            case "设置无线信道":
////                输入：表地址、无线频率（14个频率）
//                secondHandle(mCurCommandName, "表地址", "设置无线信道");
////              配置secondList数据源
////                String[] stringArray = getResources().getStringArray(R.array.rate_wrieless_arr);
//                mSecondList.addAll(Arrays.asList(getResources().getStringArray(R.array.rate_wrieless_arr)));
//                break;
            case "读硬件版本号":
//                输入：表地址
                secondHandle(mCurCommandName, "表地址");
                break;
            case "设置内条码":
//                输入：表地址、内条码（16位数字）
                secondHandle(mCurCommandName, "表地址", "内条码（16数字）");
                break;
//            case "读内条码":
////                输入：表地址
//                secondHandle(mCurCommandName, "表地址");
//                break;
            case "生产专用1":
//                输入：表地址
//                secondHandle(mCurCommandName,"表地址","手机时间");
                secondHandle(mCurCommandName, "表地址");
                break;
            case "生产专用2":
//                输入：旧表地址、新地址
                secondHandle(mCurCommandName, "表地址", "新地址");
                break;
            case "软件复位":
//                输入：表地址
                secondHandle(mCurCommandName, "表地址");
                break;
            case "水表数据补抄":
//                输入：表地址
                secondHandle(mCurCommandName, "表地址","补抄周期数");
                break;
            case "水表数据上报":
//                输入：表地址
                secondHandle(mCurCommandName, "表地址","CFG","Data");
                break;
            case "读文件数据":
//                输入：表地址
                secondHandle(mCurCommandName, "表地址","文件标识");
                break;
            case "写文件数据":
//                输入：表地址
                secondHandle(mCurCommandName, "表地址","文件标识","写文件内容");
                break;
            case "设置离散上报时间":
//                输入：表地址
                secondHandle(mCurCommandName, "表地址","离散上报时间");
                break;
            case "红外NB生产测试":
//                输入：表地址
                secondHandle(mCurCommandName, "表地址");
                break;
            case "红外读NB模组信息":
//                输入：表地址
                secondHandle(mCurCommandName, "表地址");
                break;
            case "红外触发上报":
//                输入：表地址
                secondHandle(mCurCommandName, "表地址");
                break;
            case "重启NB模组":
//                输入：表地址
                secondHandle(mCurCommandName, "表地址");
                break;
            case "读NB通信状态字":
//                输入：表地址
                secondHandle(mCurCommandName, "表地址");
                break;
            case "关闭红外":
//                输入：表地址
                secondHandle(mCurCommandName, "表地址");
                break;
            case "读IMEI":
//                输入：表地址
                secondHandle(mCurCommandName, "表地址");
                break;
            case "读IMSI":
//                输入：表地址
                secondHandle(mCurCommandName, "表地址");
                break;
            default:
                Toast.makeText(this, mCurCommandName + ":暂无相关命令操作", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 弹出输入框页面，确认信息后，组装命令然后发送出去
     *
     * @param args 变长参数
     */
    private void secondHandle(String mCurCommandName, String... args) {
        if (null != mPopCommandGridView) {
            mPopCommandGridView.hindeOrShowInputLl(false);
            mPopCommandGridView.setInputTitle(mCurCommandName);
            mPopCommandGridView.setTitles(args);
        }
    }

    /**
     * 1月--D120
     */
    public String getValueByMonth(String month) {
        return (Integer.toHexString(53535 + Integer.parseInt(month.substring(0, month.length() - 1)))).toUpperCase();
    }

    /**
     * 1日--D134 53556
     */
    public String getValueByDay(String day) {
        return (Integer.toHexString(53555 + Integer.parseInt(day.substring(0, day.length() - 1)))).toUpperCase();
    }

    /**
     * @return 无线频率--hex
     */
    public String getRateHexStrByPos(int pos) {
//        pos++;
//        String dataStr = pos+"";
        return prefix0ToLen(Integer.toHexString(pos), 2);
//        switch (pos) {
//            case 1:
////                dataStr = "005C901D";
//                dataStr = "6013";
//                break;
//            case 2:
////                dataStr = "4095621D";
//                dataStr = "4213";
//                break;
//            case 3:
////                dataStr = "C0AF681D";
//                dataStr = "4613";
//                break;
//            case 4:
////                dataStr = "40CA6E1D";
//                dataStr = "4A13";
//                break;
//            case 5:
////                dataStr = "C0E4741D";
//                dataStr = "4E13";
//                break;
//            case 6:
////                dataStr = "40FF7A1D";
//                dataStr = "5213";
//                break;
//            case 7:
////                dataStr = "C019811D";
//                dataStr = "5613";
//                break;
//            case 8:
////                dataStr = "4034871D";
//                dataStr = "5A13";
//                break;
//            case 9:
////                dataStr = "80418A1D";
//                dataStr = "5C13";
//                break;
//            case 10:
////                dataStr = "409E9F1D";
//                dataStr = "6A13";
//                break;
//            case 11:
////                dataStr = "C0B8A51D";
//                dataStr = "6E13";
//                break;
//            case 12:
////                dataStr = "40D3AB1D";
//                dataStr = "7213";
//                break;
//            case 13:
////                dataStr = "C0EDB11D";
//                dataStr = "7613";
//                break;
//            case 14:
////                dataStr = "4008B81D";
//                dataStr = "7A13";
//                break;
//            case 15:
////                dataStr = "C022BE1D";
//                dataStr = "7E13";
//                break;
//        }
//        return dataStr;
    }


    /**
     * 补0操作
     */
    public String prefix0ToLen(String originalStr, int needLen) {
        int length = originalStr.length();
        if (length >= needLen) {
            return originalStr;
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < needLen - length; i++) {
                sb.append("0");
            }
            sb.append(originalStr);
            return sb.toString();
        }
    }

    private String formatFrameNo(int frameNo) {
        String hexString = Integer.toHexString(frameNo);
        if (hexString.length() < 2) {
            return "0" + hexString;
        } else if (hexString.length() == 2) {
            return hexString;
        } else {
            return hexString.substring(0, 2);
        }
    }

    /**
     * 按字节返序
     *
     * @param originalStr 12345678
     * @return 78 56 34 12
     */
    private String reverseStringByByte(String originalStr) {
        if (null == originalStr || originalStr.length() == 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        int len = originalStr.length() / 2;
        for (int i = len; i > 0; i--) {
//            len = 4
//            6-8 4-6 2-4 0-2
            sb.append(originalStr.substring(i * 2 - 2, i * 2));
        }
        return sb.toString();
    }

    private String getCurTimeFormat() {
        return new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(new Date());
    }

    /**
     * 选择命令后，输入相关参数，构建命令帧，写入寄存器中
     */
    private void confirmBuildCommand(View view) {
        if (!mPopCommandGridView.getIsAllSelected()) {
            Toast.makeText(this, "请填写或选择相关内容后，再确认！", Toast.LENGTH_SHORT).show();
            return;
        }
        Protocol188Entity entity = new Protocol188Entity();
        entity.setEquipType("10");
        String dataStr = "";
        entity.setFrameNo(formatFrameNo(frameNo));
        if (mPopCommandGridView != null) {
//            addItemStrToList(mAddrHistList, mPopCommandGridView.getEtContentByIndex(1));
        }
        switch (mCurCommandName) {
            case "写表地址":
//                输入：旧地址、新地址
//                secondHandle(mCurCommandName,"表旧地址","新地址");
//                entity.setAddress(mPopCommandGridView.getEtContentByIndex(1));
                entity.setAddress((mPopCommandGridView.getEtContentByIndex(1)));
                entity.setControlCode("15");
                entity.setDataLen("0A");
                entity.setDataFlag("18A0");
                dataStr = reverseStringByByte(mPopCommandGridView.getEtContentByIndex(2));
                break;
            case "读计量数据":
//                输入：表地址
//                secondHandle(mCurCommandName,"表地址");
                entity.setAddress((mPopCommandGridView.getEtContentByIndex(1)));
                entity.setControlCode("01");
                entity.setDataLen("03");
                entity.setDataFlag("1F90");
                dataStr = "";
                break;
            case "读历史月累积量":
//                输入：表地址 、上1-18个月
//                secondHandle(mCurCommandName,"表地址","历史月（1-18）");
                entity.setAddress((mPopCommandGridView.getEtContentByIndex(1)));
                entity.setControlCode("01");
                entity.setDataLen("03");
                entity.setDataFlag(reverseStringByByte(mPopCommandGridView.getEtContentByIndex(2)));
                dataStr = "";
//                isNeedTwice = true;
                break;
            case "读历史日冻结量":
//                输入：表地址、上1-31日累计
//                secondHandle(mCurCommandName,"表地址","历史日（1-31）");
                entity.setAddress((mPopCommandGridView.getEtContentByIndex(1)));
                entity.setControlCode("01");
                entity.setDataLen("03");
                entity.setDataFlag(reverseStringByByte(mPopCommandGridView.getEtContentByIndex(2)));
                dataStr = "";
                break;
            case "写机电同步数据":
//                输入：表地址、XXXXXX.XX（当前累计流量）2C
//                secondHandle(mCurCommandName,"表地址","当前累计流量(8位bcd)");
                entity.setAddress((mPopCommandGridView.getEtContentByIndex(1)));
                entity.setControlCode("16");
                entity.setDataLen("08");
                entity.setDataFlag("16A0");
//                机电同步数据 格式xxxxxx.xx
                String inputNum = mPopCommandGridView.getEtContentByIndex(2);
                String floatStr = Float.parseFloat(inputNum) + "";
                if (floatStr.length() > 9) {
                    showToastLong("长度错误：当前累计流量格式xxxxxx.xx 6位整数，2位小数");
                    return;
                }
                int indexDian = floatStr.lastIndexOf(".");
                String zhengshu = prefix0ToLen(floatStr.substring(0, indexDian), 6);
                String xiaoshu = prefix0ToLen(floatStr.substring(indexDian + 1, floatStr.length()), 2);
//                dataStr = reverseStringByByte(zhengshu + xiaoshu + "2C");
                dataStr = reverseStringByByte(zhengshu + xiaoshu) + "2C";
                break;
            case "校时":
//                输入：表地址、YYYYMMDDhhmmss
//                secondHandle(mCurCommandName,"表地址");//直接将手机时间设置进去
                entity.setAddress((mPopCommandGridView.getEtContentByIndex(1)));
                entity.setControlCode("04");
                entity.setDataLen("0A");
                entity.setDataFlag("15A0");
//                YYYYMMDDhhmmss
                dataStr = getCurTimeFormat();
                break;
//            case "开阀":
////                输入：表地址
////                secondHandle(mCurCommandName,"表地址");
//                entity.setAddress((mPopCommandGridView.getEtContentByIndex(1)));
//                entity.setControlCode("04");
//                entity.setDataLen("04");
//                entity.setDataFlag("17A0");
//                dataStr = "55";
//                break;
//            case "关阀":
////                输入：表地址
////                secondHandle(mCurCommandName,"表地址");
//                entity.setAddress((mPopCommandGridView.getEtContentByIndex(1)));
//                entity.setControlCode("04");
//                entity.setDataLen("04");
//                entity.setDataFlag("17A0");
//                dataStr = "99";
//                break;
            case "出厂启用":
//                输入：表地址
//                secondHandle(mCurCommandName,"表地址");
                entity.setAddress(mPopCommandGridView.getEtContentByIndex(1));
                entity.setControlCode("04");
                entity.setDataLen("03");
                entity.setDataFlag("19A0");
                dataStr = "";
                break;
            case "设置厂内":
//                输入：表地址
//                secondHandle(mCurCommandName,"表地址");
                entity.setAddress(mPopCommandGridView.getEtContentByIndex(1));
                entity.setControlCode("04");
                entity.setDataLen("03");
                entity.setDataFlag("18A0");
                dataStr = "";
                break;
            case "读EEPROM":
//                输入：表地址、addr(2字节)、len（1字节）
//                secondHandle(mCurCommandName"表地址", "ADDR地址(四位数字)", "LEN长度(<=64)");
                entity.setAddress(mPopCommandGridView.getEtContentByIndex(1));
                entity.setControlCode("33");
                entity.setDataLen("06");
                entity.setDataFlag("10A0");
                String address = mPopCommandGridView.getEtContentByIndex(2);
                if (null == address || address.length() != 4) {
                    showToastLong("错误：请输入四位数字的ADDR地址，再操作");
                    return;
                }
                String len = mPopCommandGridView.getEtContentByIndex(3);
                if (null == len || Integer.parseInt(len) > 64) {
                    showToastLong("错误：LEN长度数值范围0-64，请重新输入后再操作");
                    return;
                }
//                dataStr = reverseStringByByte(address) + prefix0ToLen(Integer.parseInt(len,16)+"",2);
                dataStr = reverseStringByByte(address) + prefix0ToLen(Integer.toHexString(Integer.parseInt(len)), 2);
                break;
            case "读电池电压":
//                输入：表地址
//                secondHandle(mCurCommandName,"表地址");
                entity.setAddress(mPopCommandGridView.getEtContentByIndex(1));
                entity.setControlCode("33");
                entity.setDataLen("03");
                entity.setDataFlag("11A0");
                dataStr = "";
                break;
            case "清表数据":
//                输入：表地址
//                secondHandle(mCurCommandName,"表地址");
                entity.setAddress(mPopCommandGridView.getEtContentByIndex(1));
                entity.setControlCode("33");
                entity.setDataLen("03");
                entity.setDataFlag("13A0");
                dataStr = "";
                break;
            case "读软件版本号":
//                输入：表地址
//                secondHandle(mCurCommandName,"表地址");
                entity.setAddress(mPopCommandGridView.getEtContentByIndex(1));
                entity.setControlCode("33");
                entity.setDataLen("03");
                entity.setDataFlag("1AA0");
                dataStr = "";
                break;
            case "读表运行数据":
//                输入：表地址
//                secondHandle(mCurCommandName,"表地址");
                entity.setAddress(mPopCommandGridView.getEtContentByIndex(1));
                entity.setControlCode("33");
                entity.setDataLen("03");
                entity.setDataFlag("1BA0");
                dataStr = "";
//                isNeedTwice = true;
                break;
            case "读Boot版本号":
//                输入：表地址
//                secondHandle(mCurCommandName,"表地址");
                entity.setAddress(mPopCommandGridView.getEtContentByIndex(1));
                entity.setControlCode("33");
                entity.setDataLen("03");
                entity.setDataFlag("1CA0");
                dataStr = "";
                break;
//            case "设置无线信道":
////                输入：表地址、无线频率（14个频率）
////                secondHandle(mCurCommandName,"表地址","设置无线信道");
//                entity.setAddress(mPopCommandGridView.getEtContentByIndex(1));
//                entity.setControlCode("33");
//                entity.setDataLen("04");
//                entity.setDataFlag("1DA0");
//                dataStr = mPopCommandGridView.getEtContentByIndex(2);
//                break;
            case "读硬件版本号":
//                输入：表地址
//                secondHandle(mCurCommandName,"表地址");
                entity.setAddress(mPopCommandGridView.getEtContentByIndex(1));
                entity.setControlCode("33");
                entity.setDataLen("03");
                entity.setDataFlag("1EA0");
                dataStr = "";
                break;
            case "设置内条码":
//                输入：表地址、内条码（16位数字）
//                secondHandle(mCurCommandName,"表地址","内条码（16数字）");
                entity.setAddress(mPopCommandGridView.getEtContentByIndex(1));
                entity.setControlCode("33");
                entity.setDataLen("0B");
                entity.setDataFlag("1FA0");
                String contentNeiTiaoMa = mPopCommandGridView.getEtContentByIndex(2);
                if (null == contentNeiTiaoMa || contentNeiTiaoMa.length() != 16){
                    Toast.makeText(this,"长度错误：内条码是16位数字，请重新输入！",Toast.LENGTH_LONG).show();
                    return;
                }
                dataStr = mPopCommandGridView.getEtContentByIndex(2);
                break;
//            case "读内条码":
////                输入：表地址
////                secondHandle(mCurCommandName,"表地址");
//                entity.setAddress(mPopCommandGridView.getEtContentByIndex(1));
//                entity.setControlCode("33");
//                entity.setDataLen("03");
//                entity.setDataFlag("1FA0");
//                dataStr = "0000";
//                break;
            case "生产专用1":
//                输入：表地址
//                secondHandle(mCurCommandName,"表地址");
                entity.setAddress(mPopCommandGridView.getEtContentByIndex(1));
                entity.setControlCode("33");
                entity.setDataLen("0A");
                entity.setDataFlag("20A0");
                dataStr = getCurTimeFormat();
                break;
            case "生产专用2":
//                输入：旧表地址、新地址
//                secondHandle(mCurCommandName,"表地址","新地址");
                entity.setAddress(mPopCommandGridView.getEtContentByIndex(1));
                entity.setControlCode("33");
                entity.setDataLen("0A");
                entity.setDataFlag("21A0");
                dataStr = reverseStringByByte(mPopCommandGridView.getEtContentByIndex(2));
                break;
            case "软件复位":
//                输入：表地址
//                secondHandle(mCurCommandName,"表地址");
                entity.setAddress(mPopCommandGridView.getEtContentByIndex(1));
                entity.setControlCode("33");
                entity.setDataLen("03");
                entity.setDataFlag("22A0");
                dataStr = "";
                break;
            case "水表数据补抄":
//                输入：表地址
//                secondHandle(mCurCommandName, "表地址","补抄周期数");
                entity.setAddress(mPopCommandGridView.getEtContentByIndex(1));
                entity.setControlCode("33");
                entity.setDataLen("03");
                entity.setDataFlag("23A0");
                dataStr = "";
                break;
            case "水表数据上报":
//                输入：表地址
//                secondHandle(mCurCommandName,"表地址");
                entity.setAddress(mPopCommandGridView.getEtContentByIndex(1));
                entity.setControlCode("33");
                entity.setDataLen("03");
                entity.setDataFlag("22A0");
                dataStr = "";
                break;
            case "读文件数据":
//                输入：表地址
//                secondHandle(mCurCommandName,"表地址");
                entity.setAddress(mPopCommandGridView.getEtContentByIndex(1));
                entity.setControlCode("33");
                entity.setDataLen("03");
                entity.setDataFlag("22A0");
                dataStr = "";
                break;
            case "写文件数据":
//                输入：表地址
//                secondHandle(mCurCommandName,"表地址");
                entity.setAddress(mPopCommandGridView.getEtContentByIndex(1));
                entity.setControlCode("33");
                entity.setDataLen("03");
                entity.setDataFlag("22A0");
                dataStr = "";
                break;
            case "设置离散上报时间":
//                输入：表地址
//                secondHandle(mCurCommandName,"表地址");
                entity.setAddress(mPopCommandGridView.getEtContentByIndex(1));
                entity.setControlCode("33");
                entity.setDataLen("03");
                entity.setDataFlag("22A0");
                dataStr = "";
                break;
            case "红外NB生产测试":
//                输入：表地址
//                secondHandle(mCurCommandName,"表地址");
                entity.setAddress(mPopCommandGridView.getEtContentByIndex(1));
                entity.setControlCode("33");
                entity.setDataLen("03");
                entity.setDataFlag("22A0");
                dataStr = "";
                break;
            case "红外读NB模组信息":
//                输入：表地址
//                secondHandle(mCurCommandName,"表地址");
                entity.setAddress(mPopCommandGridView.getEtContentByIndex(1));
                entity.setControlCode("33");
                entity.setDataLen("03");
                entity.setDataFlag("22A0");
                dataStr = "";
                break;
                case "红外触发上报":
//                输入：表地址
//                secondHandle(mCurCommandName,"表地址");
                entity.setAddress(mPopCommandGridView.getEtContentByIndex(1));
                entity.setControlCode("33");
                entity.setDataLen("03");
                entity.setDataFlag("22A0");
                dataStr = "";
                break;
                case "重启NB模组":
//                输入：表地址
//                secondHandle(mCurCommandName,"表地址");
                entity.setAddress(mPopCommandGridView.getEtContentByIndex(1));
                entity.setControlCode("33");
                entity.setDataLen("03");
                entity.setDataFlag("22A0");
                dataStr = "";
                break;
            case "读NB通信状态字":
//                输入：表地址
//                secondHandle(mCurCommandName,"表地址");
                entity.setAddress(mPopCommandGridView.getEtContentByIndex(1));
                entity.setControlCode("33");
                entity.setDataLen("03");
                entity.setDataFlag("22A0");
                dataStr = "";
                break;
            case "关闭红外":
//                输入：表地址
//                secondHandle(mCurCommandName,"表地址");
                entity.setAddress(mPopCommandGridView.getEtContentByIndex(1));
                entity.setControlCode("33");
                entity.setDataLen("03");
                entity.setDataFlag("22A0");
                dataStr = "";
                break;
            case "读IMEI":
//                输入：表地址
//                secondHandle(mCurCommandName,"表地址");
                entity.setAddress(mPopCommandGridView.getEtContentByIndex(1));
                entity.setControlCode("33");
                entity.setDataLen("03");
                entity.setDataFlag("22A0");
                dataStr = "";
                break;
            case "读IMSI":
//                输入：表地址
//                secondHandle(mCurCommandName,"表地址");
                entity.setAddress(mPopCommandGridView.getEtContentByIndex(1));
                entity.setControlCode("33");
                entity.setDataLen("03");
                entity.setDataFlag("22A0");
                dataStr = "";
                break;
            default:
                Toast.makeText(this, mCurCommandName + ":暂无相关命令操作", Toast.LENGTH_SHORT).show();
                return;
        }
        entity.setDataStr(dataStr);
        mSendDataByteArr = entity.rebuildHex2ByteArr();
       sendCommand();
        hideSoftInputLayout(view);
    }

    private void hideSoftInputLayout(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }


    /**
     * 添加发送帧
     */
    private void addSendDataItem(String hexStr) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hexStr.length() / 2; i++) {
            sb.append(hexStr.substring(2 * i, 2 * i + 2));
            sb.append(" ");
        }
        CommondSendResponseEntity entity = new CommondSendResponseEntity();
        entity.setCommandStr(sb.toString());
        entity.setTitle(mCurCommandName);
        mDataList.add(entity);
        if (null != mCommandListAdapter) {
            mCommandListAdapter.notifyDataSetChanged();
            mCommandHistoryLv.setSelection(mDataList.size() - 1);
        }
    }


    /**
     * 添加响应解析帧
     */
    private void addReceiveDataItem(String hexStr) {
        StringBuilder sb = new StringBuilder();
        StringBuilder resolveSb = new StringBuilder();
        int hexLen = hexStr.length();
        for (int i = 0; i < hexLen / 2; i++) {
            sb.append(hexStr.substring(2 * i, 2 * i + 2));
            sb.append(" ");
        }
        CommondSendResponseEntity entity = new CommondSendResponseEntity();
        entity.setTitle("解析帧");
        entity.setIsRes(true);
        entity.setCommandStr(sb.toString());

//                              681303015704FFFFFFFF01000080330A57046B07986B
        //                    68 13 03 01 	57 04	 FF FF FF FF	 01 00 00 80 	33 0A 57 04 6B 07 98 6B
//                                                                                                    Rx Tx

//        String signal = "\n信号强度-接受:" + Integer.parseInt(hexStr.substring(hexLen - 4, hexLen - 2), 16) + "、发送:" + Integer.parseInt(hexStr.substring(hexLen - 2, hexLen), 16);
        String signal = "";


//        在此解析命令意义，并显示： 188协议最小长度 32 【68开始，16结束（有时不以16结尾）】 依据“数据标识解析”
//        if (hexStr.length() >= 32 && hexStr.substring(0,2).equals("68") && hexStr.substring(hexStr.length() - 3,hexStr.length() - 1).equals("16")){
        if (hexStr.length() >= 32 && hexStr.substring(0, 2).equals("68")) {
            String controlCode = (hexStr.substring(18, 20));
            String dataLen = (hexStr.substring(20, 22));
            String dataFlag = hexStr.substring(22, 26);
            switch (dataFlag) {
//                响应读表地址
                case "0A81":
                    resolveSb.append("响应读表地址: \n[");
                    String meterAddr = reverseStringByByte(hexStr.substring(4, 18));
                    resolveSb.append("表地址：" + meterAddr);
                    resolveSb.append("]");
                    resolveSb.append(signal);
                    break;
//                 响应写表地址   响应设置厂内扩展命令
                case "18A0":
                    String meterAddrWrite = reverseStringByByte(hexStr.substring(4, 18));
                    if (controlCode.equals("95")) {
                        resolveSb.append("响应写表地址:  \n[");
                        resolveSb.append("成功 ; 新地址：" + meterAddrWrite);
                    } else if (controlCode.equals("D5")) {
                        resolveSb.append("响应写表地址: \n[");
                        resolveSb.append("失败 ;原地址：" + meterAddrWrite);
                    } else if (controlCode.equals("84")) {
                        resolveSb.append("响应设置厂内扩展命令:  \n[");
                        resolveSb.append("成功;");
                    } else if (controlCode.equals("C4")) {
                        resolveSb.append("响应设置厂内扩展命令:  \n[");
                        resolveSb.append("失败; ");
                    }
                    resolveSb.append("]");
                    resolveSb.append(signal);
                    break;
//                    响应读计量数据
//                68 10 69 00 01 07 17 20 00 81 16 1F 90 02 00 00 00 00 2C 00 00 00 00 2C 59 54 16 19 08 17 20 13 01 EF 16 52 4F
                case "1F90":
                    resolveSb.append("响应读计量数据:  \n[");
                    if (hexStr.length() < 66) {
                        resolveSb.append("长度异常; ");
                        return;
                    }
                    String curTotalFlow = reverseStringByByte(hexStr.substring(28, 36));
                    String curDayFlow = reverseStringByByte(hexStr.substring(38, 46));
                    String curTime = reverseStringByByte(hexStr.substring(48, 62));
                    String meterStatus = reverseStringByByte(hexStr.substring(62, 66));
                    if (controlCode.equals("81")) {
                        resolveSb.append("成功; ");
                        resolveSb.append("; 当前累计流量：" + Integer.parseInt(curTotalFlow.substring(0, 6)) + "." + Integer.parseInt(curTotalFlow.substring(6, 8)));
                        resolveSb.append("; 结算日累积量：" + Integer.parseInt(curDayFlow.substring(0, 6)) + "." + Integer.parseInt(curDayFlow.substring(6, 8)));
                        resolveSb.append("; 实时时间：");
                        resolveSb.append("; " + curTime.substring(0, 4) + "-" + curTime.substring(4, 6) + "-" + curTime.substring(6, 8) + " " + curTime.substring(8, 10) + ":" + curTime.substring(10, 12) + ":" + curTime.substring(12, 14));
                        resolveSb.append("; 表状态：" + meterStatus.substring(0, 2) + " " + meterStatus.substring(2, 4));
                    } else if (controlCode.equals("C1")) {
                        resolveSb.append("失败; ");
                    }
                    resolveSb.append("]");
                    resolveSb.append(signal);
                    break;
//                响应读历史月结算日累积量
                case "20D1":
                case "21D1":
                case "22D1":
                case "23D1":
                case "24D1":
                case "25D1":
                case "26D1":
                case "27D1":
                case "28D1":
                case "29D1":
                case "2AD1":
                case "2BD1":
                case "2CD1":
                case "2DD1":
                case "2ED1":
                case "2FD1":
                case "30D1":
                case "31D1":
                    resolveSb.append("响应读历史月结算日累积量:  \n[");
                    String totalFow = reverseStringByByte(hexStr.substring(28, 36));
                    int offset = Integer.parseInt(dataFlag.substring(0, 2), 16) - 0x20 + 1;
                    if (controlCode.equals("81")) {
                        resolveSb.append("成功;");
                        resolveSb.append("; 上" + offset + "月结算日累积量:" + Integer.parseInt(totalFow.substring(0, 6)) + "." + Integer.parseInt(totalFow.substring(6, 8)));
                    } else if (controlCode.equals("C1")) {
                        resolveSb.append("失败; ");
                    }
                    resolveSb.append("]");
                    resolveSb.append(signal);
                    break;
//                响应读历史日冻结累计流量
                case "34D1":
                case "35D1":
                case "36D1":
                case "37D1":
                case "38D1":
                case "39D1":
                case "3AD1":
                case "3BD1":
                case "3CD1":
                case "3DD1":
                case "3ED1":
                case "3FD1":
                case "40D1":
                case "41D1":
                case "42D1":
                case "43D1":
                case "44D1":
                case "45D1":
                case "46D1":
                case "47D1":
                case "48D1":
                case "49D1":
                case "4AD1":
                case "4BD1":
                case "4CD1":
                case "4DD1":
                case "4ED1":
                case "4FD1":
                case "50D1":
                case "51D1":
                case "52D1":
                    resolveSb.append("响应读历史日冻结累计流量:  \n[");
                    String totalFowFreeze = reverseStringByByte(hexStr.substring(28, 36));
                    int offsetDay = Integer.parseInt(dataFlag.substring(0, 2), 16) - 0x34 + 1;
                    if (controlCode.equals("81")) {
                        resolveSb.append("成功;");
                        resolveSb.append("; 上" + offsetDay + "日冻结累积流量:" + Integer.parseInt(totalFowFreeze.substring(0, 6)) + "." + Integer.parseInt(totalFowFreeze.substring(6, 8)));
                    } else if (controlCode.equals("C1")) {
                        resolveSb.append("失败; ");
                    }
                    resolveSb.append("]");
                    resolveSb.append(signal);
                    break;
//                   响应写机电同步数据
                case "16A0":
                    resolveSb.append("响应写机电同步数据: \n[");
                    String meterStatusJd = reverseStringByByte(hexStr.substring(30, 34));
                    if (controlCode.equals("96")) {
                        resolveSb.append("成功;");
                        resolveSb.append("表状态;" + meterStatusJd.substring(0, 2) + " " + meterStatusJd.substring(2, 4));
                    } else if (controlCode.equals("D6")) {
                        resolveSb.append("失败; ");
                    }
                    resolveSb.append("]");
                    resolveSb.append(signal);
                    break;
//                    响应校时
                case "15A0":
                    resolveSb.append("响应校时:  \n[");
                    if (controlCode.equals("84")) {
                        resolveSb.append("成功;");
                    } else if (controlCode.equals("C4")) {
                        resolveSb.append("失败; ");
                    }
                    resolveSb.append("]");
                    resolveSb.append(signal);
                    break;
//                    响应开阀
//                case "17A0":
////                    String resData = hexStr.substring(28, 30);
//                    resolveSb.append("响应" + commandStr + ":  \n[");
//                    if (controlCode.equals("84")) {
//                        resolveSb.append("成功;");
//                    } else if (controlCode.equals("C4")) {
//                        resolveSb.append("失败; ");
//                    }
//                    resolveSb.append("]");
//                    resolveSb.append(signal);
//                    break;
//                    响应出厂启用
                case "19A0":
                    resolveSb.append(" 响应出厂启用:  \n[");
                    if (controlCode.equals("84")) {
                        resolveSb.append("成功;");
                    } else if (controlCode.equals("C4")) {
                        resolveSb.append("失败; ");
                    }
                    resolveSb.append("]");
                    resolveSb.append(signal);
                    break;
//                响应读EEPROM
                case "10A0":
                    int dataOffset = (Integer.parseInt(dataLen, 16) - 3) * 2;
                    String data = hexStr.substring(28, 28 + dataOffset);
                    resolveSb.append(" 响应读EEPROM:  \n[");
                    if (controlCode.equals("B3")) {
                        resolveSb.append("成功;");
                        resolveSb.append("EEPROM:" + data);
                    } else if (controlCode.equals("F3")) {
                        resolveSb.append("失败; ");
                    }
                    resolveSb.append("]");
                    resolveSb.append(signal);
                    break;
//                响应读电池电压
                case "11A0":
//                    int bateryVoll = Integer.parseInt(reverseStringByByte(hexStr.substring(30,34)));
                    int bateryVoll = Integer.parseInt(reverseStringByByte(hexStr.substring(28, 32)), 16);
                    resolveSb.append(" 响应读电池电压:  \n[");
                    if (controlCode.equals("B3")) {
                        resolveSb.append("成功;");
                        resolveSb.append("电池电压:" + bateryVoll + " (10mV)");
                    } else if (controlCode.equals("F3")) {
                        resolveSb.append("失败; ");
                    }
                    resolveSb.append("]");
                    resolveSb.append(signal);
                    break;
//                响应读软件版本号
                case "1AA0":
//                    String softwareV = hexStr.substring(28, 30) + "-" + hexStr.substring(30, 32) + "-" + hexStr.substring(32, 34) + "-" + hexStr.substring(34, 36) + "-" + hexStr.substring(36, 38);
                    String softwareV = hexStr.substring(34, 36) + "-" + hexStr.substring(32, 34) + "-" + hexStr.substring(30, 32) + "-" + hexStr.substring(28, 30) + "-" + hexStr.substring(36, 38);
                    resolveSb.append(" 响应读软件版本号: \n[");
                    if (controlCode.equals("B3")) {
                        resolveSb.append("成功;");
                        resolveSb.append("软件版本号:" + softwareV);
                    } else if (controlCode.equals("F3")) {
                        resolveSb.append("失败; ");
                    }
                    resolveSb.append("]");
                    resolveSb.append(signal);
                    break;
//                响应读表运行数据
                case "1BA0":
                    resolveSb.append(" 响应读表运行数据:  \n[");
                    if (controlCode.equals("B3")) {
                        if (hexStr.length() < 72) {
                            resolveSb.append(" 数据长度异常 ; ");
                            return;
                        }
                        String dataContentStr = hexStr.substring(28, 72);
                        resolveSb.append("成功;");
                        resolveSb.append("; 单边累计流量:" + Integer.parseInt(reverseStringByByte(dataContentStr.substring(0, 8)), 16));
                        resolveSb.append("; 开阀次数:" + Integer.parseInt(reverseStringByByte(dataContentStr.substring(8, 12)), 16));
                        resolveSb.append("; 关阀次数:" + Integer.parseInt(reverseStringByByte(dataContentStr.substring(12, 16)), 16));
                        resolveSb.append("; 强磁次数:" + Integer.parseInt(reverseStringByByte(dataContentStr.substring(16, 20)), 16));
                        resolveSb.append("; 广播命令次数:" + Integer.parseInt(reverseStringByByte(dataContentStr.substring(20, 24)), 16));
                        resolveSb.append("; 广播点名次数:" + Integer.parseInt(reverseStringByByte(dataContentStr.substring(24, 28)), 16));
                        resolveSb.append("; 点抄次数:" + Integer.parseInt(reverseStringByByte(dataContentStr.substring(28, 32)), 16));
                        resolveSb.append("; 电池电压:" + Integer.parseInt(reverseStringByByte(dataContentStr.substring(32, 36)), 16) + "(10mV)");
                        resolveSb.append("; 表运行时间:" + Long.parseLong(reverseStringByByte(dataContentStr.substring(36, 44)), 16) + "(秒)");
                    } else if (controlCode.equals("F3")) {
                        resolveSb.append(" 失败 ; ");
                    }
                    resolveSb.append("]");
                    resolveSb.append(signal);
                    break;
//                响应读Boot版本号
                case "1CA0":
//                    String bootV = hexStr.substring(30, 38);
                    String bootV = hexStr.substring(28, 36);
                    resolveSb.append(" 响应读Boot版本号:  \n[");
                    if (controlCode.equals("B3")) {
                        resolveSb.append("成功;");
                        resolveSb.append(" Boot版本号:" + bootV.substring(6, 8) + "-" + bootV.substring(4, 6) + "-" + bootV.substring(2, 4) + "-" + bootV.substring(0, 2));
                    } else if (controlCode.equals("F3")) {
                        resolveSb.append(" 失败 ; ");
                    }
                    resolveSb.append("]");
                    resolveSb.append(signal);
                    break;
//                响应设置无线信道
                case "1DA0":
                    resolveSb.append(" 响应设置无线信道:  \n[");
                    if (controlCode.equals("B3")) {
                        resolveSb.append("成功;");
                    } else if (controlCode.equals("F3")) {
                        resolveSb.append(" 失败 ; ");
                    }
                    resolveSb.append("]");
                    resolveSb.append(signal);
                    break;
//                响应读硬件版本号
                case "1EA0":
                    String hardwareV =
                            (char) (Integer.parseInt(hexStr.substring(36, 38), 16)) + ""
                                    + (char) (Integer.parseInt(hexStr.substring(34, 36), 16)) + ""
                                    + (char) (Integer.parseInt(hexStr.substring(32, 34), 16)) + ""
                                    + (char) (Integer.parseInt(hexStr.substring(30, 32), 16)) + ""
                                    + (char) (Integer.parseInt(hexStr.substring(28, 30), 16)) + "";

                    resolveSb.append(" 响应读硬件版本号:  \n[");
                    if (controlCode.equals("B3")) {
                        resolveSb.append("成功;");
                        resolveSb.append("硬件版本号;" + hardwareV);
                    } else if (controlCode.equals("F3")) {
                        resolveSb.append(" 失败 ; ");
                    }
                    resolveSb.append("]");
                    resolveSb.append(signal);
                    break;
//                响应设置内条码
                case "1FA0":
                    resolveSb.append(" 响应设置内条码:  \n[");
                    if (controlCode.equals("B3")) {
                        resolveSb.append("成功;");
                    } else if (controlCode.equals("F3")) {
                        resolveSb.append(" 失败 ; ");
                    }
                    resolveSb.append("]");
                    resolveSb.append(signal);
                    break;
//                响应生产专用1
                case "20A0":
                    resolveSb.append(" 响应生产专用1:  \n[");
                    if (hexStr.length() < 70) {
                        resolveSb.append("数据长度异常！;");
                        return;
                    }
                    if (controlCode.equals("B3")) {
                        resolveSb.append("成功;");
                        String dataContentStr = hexStr.substring(28, 70);
//                        String timeStr = reverseStringByByte(dataContentStr.substring(16,30));
                        resolveSb.append("成功;");
                        resolveSb.append("; 当前累计流量:" + Integer.parseInt(reverseStringByByte(dataContentStr.substring(0, 6))) + "." + Integer.parseInt(reverseStringByByte(dataContentStr.substring(6, 8))));
                        resolveSb.append("; 结算日累积量:" + Integer.parseInt(reverseStringByByte(dataContentStr.substring(10, 16))) + "." + Integer.parseInt(reverseStringByByte(dataContentStr.substring(16, 18))));
                        resolveSb.append("; 实时时间:" + dataContentStr.substring(20, 24) + "-" + dataContentStr.substring(24, 26) + "-" + dataContentStr.substring(26, 28) + " " + dataContentStr.substring(28, 30) + ":" + dataContentStr.substring(30, 32) + ":" + dataContentStr.substring(32, 34));
                        resolveSb.append("; 表状态:" + dataContentStr.substring(34, 36) + " " + dataContentStr.substring(36, 38));
                        resolveSb.append("; 电池电压:" + Integer.parseInt(reverseStringByByte(dataContentStr.substring(38, 42)), 16) + " (10mV)");
                    } else if (controlCode.equals("F3")) {
                        resolveSb.append(" 失败 ; ");
                    }
                    resolveSb.append("]");
                    resolveSb.append(signal);
                    break;
//                响应生产专用2
                case "21A0":
                    resolveSb.append(" 响应生产专用2:  \n[");
                    if (hexStr.length() <= 79) {
                        resolveSb.append("数据长度异常！;");
                        return;
                    }
                    if (controlCode.equals("B3")) {
                        resolveSb.append("成功;");
                        String dataContentStr = hexStr.substring(28, 80);
//                        String timeStr = reverseStringByByte(dataContentStr.substring(16,30));
                        String softwareV2 = (dataContentStr.substring(dataContentStr.length() - 11, dataContentStr.length() - 1));
                        resolveSb.append("成功;");

                        resolveSb.append("; 当前累计流量:" + Integer.parseInt(reverseStringByByte(dataContentStr.substring(0, 6))) + "." + Integer.parseInt(reverseStringByByte(dataContentStr.substring(6, 8))));
                        resolveSb.append("; 结算日累积量:" + Integer.parseInt(reverseStringByByte(dataContentStr.substring(10, 16))) + "." + Integer.parseInt(reverseStringByByte(dataContentStr.substring(16, 18))));
                        resolveSb.append("; 实时时间:" + dataContentStr.substring(20, 24) + "-" + dataContentStr.substring(24, 26) + "-" + dataContentStr.substring(26, 28) + " " + dataContentStr.substring(28, 30) + ":" + dataContentStr.substring(30, 32) + ":" + dataContentStr.substring(32, 34));
                        resolveSb.append("; 表状态:" + dataContentStr.substring(34, 36) + " " + dataContentStr.substring(36, 38));
                        resolveSb.append("; 电池电压:" + Integer.parseInt(reverseStringByByte(dataContentStr.substring(38, 42)), 16) + " (10mV)");

//                        resolveSb.append("; 当前累计流量:"+Integer.parseInt(reverseStringByByte(dataContentStr.substring(0,6))+"."+Integer.parseInt(reverseStringByByte(dataContentStr.substring(6,8)))));
//                        resolveSb.append("; 结算日累积量:"+Integer.parseInt(reverseStringByByte(dataContentStr.substring(8,14))+"."+Integer.parseInt(reverseStringByByte(dataContentStr.substring(14,16)))));
//                        resolveSb.append("; 实时时间:"+timeStr.substring(0,4)+"-"+timeStr.substring(4,6)+"-"+timeStr.substring(6,8)+" "+timeStr.substring(8,10)+":"+timeStr.substring(10,12)+":"+timeStr.substring(12,14));
//                        resolveSb.append("; 表状态:"+dataContentStr.substring(30,32)+" "+dataContentStr.substring(32,34));
//                        resolveSb.append("; 电池电压:"+Integer.parseInt(reverseStringByByte(dataContentStr.substring(34,38)),16)+" (10mV)");
                        resolveSb.append("; 软件版本号:" + softwareV2.substring(8, 10) + "-" + softwareV2.substring(6, 8) + "-" + softwareV2.substring(4, 6) + "-" + softwareV2.substring(2, 4) + "-" + softwareV2.substring(0, 2));
                    } else if (controlCode.equals("F3")) {
                        resolveSb.append(" 失败 ; ");
                    }
                    resolveSb.append("]");
                    resolveSb.append(signal);
                    break;
//                响应软件复位
                case "22A0":
                    resolveSb.append(" 响应软件复位:  \n[");
                    if (controlCode.equals("B3")) {
                        resolveSb.append("成功;");
                    } else if (controlCode.equals("F3")) {
                        resolveSb.append(" 失败 ; ");
                    }
                    resolveSb.append("]");
                    resolveSb.append(signal);
                    break;
            }
        }

        entity.setResolveStr(resolveSb.toString());
        mDataList.add(entity);
        if (null != mCommandListAdapter) {
            mCommandListAdapter.notifyDataSetChanged();
            mCommandHistoryLv.setSelection(mDataList.size() - 1);
        }
    }



//    begin CommandsListViewAdapter

    private class CommandsListViewAdapter extends BaseAdapter {

        LayoutInflater mInflater = LayoutInflater.from(ST25DVCommandActivity.this);

        @Override
        public int getCount() {
            return mDataList == null ? 0 : mDataList.size();
        }

        @Override
        public Object getItem(int position) {
            return mDataList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if (null == convertView) {
                convertView = mInflater.inflate(R.layout.item_command_st25dv_lv, parent, false);
                viewHolder = new ViewHolder();
                viewHolder.mTitleTv = convertView.findViewById(R.id.title_command_name_tv);
                viewHolder.mCommandResolveTv = convertView.findViewById(R.id.command_resolve_tv);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            CommondSendResponseEntity entity = mDataList.get(position);

//            发送帧
            if (!entity.getIsRes()) {
                viewHolder.mTitleTv.setText(entity.getTitle()+" (" + entity.getTimeStamp() + ")");
                viewHolder.mCommandResolveTv.setText(entity.getCommandStr());
//            响应帧
            } else {
                viewHolder.mTitleTv.setText("响应"+entity.getTitle()+"(" + entity.getTimeStamp() + ")");
                viewHolder.mCommandResolveTv.setText(entity.getCommandStr() + "\n" + entity.getResolveStr());
            }

            return convertView;
        }

        class ViewHolder {
            TextView mTitleTv;
            TextView mCommandResolveTv;
        }

    }


//    end CommandsListViewAdapter




}

