using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Globalization;
using System.IO;
using System.Linq;
using System.Net.Sockets;
using System.Runtime.InteropServices;
using System.Text;
using System.Threading;
using System.Windows.Forms;

namespace SeuicDecode
{
    public partial class Form1 : Form
    {
        public static string photo_path = System.Configuration.ConfigurationManager.AppSettings["photo_path"].Trim();
        public static string log_path = System.Configuration.ConfigurationManager.AppSettings["log_path"].Trim();
        public static TcpClient client;
        public static NetworkStream netWStream;
        //public static byte[] recv_buffer;
        Thread thread;
        public static bool m_Measure_thread_exit = false;
        object objviewlocker = new object();

        public Form1()
        {
            InitializeComponent();
        }

        //启动
        private void btnStart_Click(object sender, EventArgs e)
        {
            if ((client != null) && (client.Connected == true))
            {
                netWStream = client.GetStream();
                netWStream.ReadTimeout = 1000;
                netWStream.WriteTimeout = 1000;
                thread = new Thread(MeasureThread);
                thread.Start();
            }
            else
            {
                try
                {
                    client = new TcpClient(System.Configuration.ConfigurationManager.AppSettings["ipaddress"].Trim(), 7777);
                }
                catch (Exception ex)
                {
                    MessageBox.Show(ex.Message);
                    LogWrite("SocketException e " + e);
                    return;
                }

                netWStream = client.GetStream();
                netWStream.ReadTimeout = 1000;
                netWStream.WriteTimeout = 1000;
                thread = new Thread(MeasureThread);
                thread.Start();
            }
            btnStop.Enabled = true;
            btnStart.Enabled = false;
        }
        [StructLayout(LayoutKind.Sequential, Pack = 1)]
        public struct XData
        {
            public string barcode;
            public string imgname;
            public Image img;
            public string scantime;
            public string error_msg;
        };
        XData xdata;
        public delegate void MeasureInvoke();
        int totalcount = 0;

        #region MeasureThread
        private void MeasureThread()
        {
            m_Measure_thread_exit = false;

            while (!m_Measure_thread_exit)
            {
                xdata = new XData();
                bool success = recieve_data(ref xdata);
                if (!success || !string.IsNullOrEmpty(xdata.error_msg))
                {
                    //LogWrite("Receive data failed: " + xdata.error_msg);
                    Thread.Sleep(30);
                    continue;
                }

                MeasureInvoke inInvoke = new MeasureInvoke(UpdateMeasureInfo);
                try
                {
                    this.Invoke(inInvoke);
                    //BeginInvoke(inInvoke);
                }
                catch (Exception e)
                {
                    LogWrite("MeasureThread catch");
                    LogWrite(e.ToString());
                }
            }

            LogWrite("MeasureThread exit!");
            m_Measure_thread_exit = true;
            client.Close();
        }
        #endregion

        #region UpdateMeasureInfo
        private void UpdateMeasureInfo()
        {
            lock (objviewlocker)
            {
                string msg = xdata.scantime + "\t" + xdata.barcode + "\t" + xdata.imgname;
                LogData(msg);//保存文件
                txtBarcode.Text = xdata.barcode;
                listRecord.Items.Insert(0, xdata.scantime + "\t" + xdata.barcode);
                totalcount++;
                txtCount.Text = totalcount.ToString();
                if (!Directory.Exists(Path.Combine(photo_path, DateTime.Now.ToString("yyyyMMdd"))))
                {
                    Directory.CreateDirectory(Path.Combine(photo_path, DateTime.Now.ToString("yyyyMMdd")));
                }
                if (!Directory.Exists(Path.Combine(Path.Combine(photo_path, DateTime.Now.ToString("yyyyMMdd")), DateTime.Now.ToString("HH"))))
                {
                    Directory.CreateDirectory(Path.Combine(Path.Combine(photo_path, DateTime.Now.ToString("yyyyMMdd")), DateTime.Now.ToString("HH")));
                }
                //保存图片
                xdata.img.Save(Path.Combine(Path.Combine(Path.Combine(photo_path, DateTime.Now.ToString("yyyyMMdd")), DateTime.Now.ToString("HH")), xdata.imgname));
                pictureboxBMP.Image = xdata.img;
            }
        }
        #endregion

        public bool recieve_data(ref XData xdata)
        {
            bool check = parse_stream(ref xdata);
            if (check == true)
            {
                if (string.IsNullOrEmpty(xdata.barcode))
                {
                    LogWrite("无条码");
                    xdata.error_msg = "无条码";
                    return false;
                }
                else
                {
                    return true;
                    //recv_buffer = new byte[xdata.length];
                    //Buffer.BlockCopy(buf, 0, recv_buffer, 0, (int)xdata.length);
                }
                //return buf.Length;
            }

            return false;
        }
        public bool parse_stream(ref XData xdata)
        {
            byte[] buf = new byte[18];

            #region 检测数据包头0xFEFE
            // check StartCode
            int goodIdx = 0;
            int received = -1;
            int time_count = 100;

            while (true)
            {
                try
                {
                    received = netWStream.Read(buf, 0, 1);

                    //var hex = BitConverter.ToString(buf, 0, 1).Replace("-", string.Empty).ToLower();
                    //LOGI("Read：");
                    //Rabb.Core.Utility.LogManager.DefaultLogger.Info(hex);
                }
                catch (Exception ex)
                {
                    LogWrite("Read Exception " + ex.ToString());
                    xdata.error_msg = "Read Exception " + ex.ToString();
                    return false;
                }
                if (received > 0)
                {
                    if (buf[0] == 0xFE)
                    {
                        if (goodIdx == 1)
                        {
                            break;
                        }
                        else
                        {
                            goodIdx++;
                        }
                    }
                    else
                    {
                        goodIdx = 0;
                    }
                }
                Thread.Sleep(10);
                if (time_count-- <= 0)
                {
                    xdata.error_msg = "Read 0xFE time out ";
                    LogWrite("Read 0xFE time out ");
                    return false;
                }
            }
            #endregion

            #region 取条码长度
            byte[] temp_revbuf = new byte[18];
            int received_Total = 0;
            int remain_length = buf.Length;//已收文件头被覆盖
            time_count = 100;
            while (true)
            {
                if (remain_length > 0)
                {
                    try
                    {
                        received = netWStream.Read(temp_revbuf, 0, remain_length);
                        Buffer.BlockCopy(temp_revbuf, 0, buf, received_Total, received);
                        received_Total += received;
                        remain_length -= received;
                    }
                    catch (Exception ex)
                    {
                        LogWrite("Read Header Exception " + ex.ToString());
                        xdata.error_msg = "Read Header Exception " + ex.ToString();
                        return false;
                    }
                }
                else
                {
                    LogWrite("received_Total == " + received_Total.ToString());
                    break;
                }
                Thread.Sleep(10);
                if (time_count-- <= 0)
                {
                    LogWrite("Read Header time out ");
                    xdata.error_msg = "Read Header time out ";
                    return false;
                }
            }

            if (received_Total != buf.Length)
            {
                LogWrite("received_Total != 18");
                xdata.error_msg = "received_Total != 18";
                return false;
            }
            #endregion

            #region 读条码
            byte[] bufbarcodelen = new byte[4];
            Buffer.BlockCopy(buf, 14, bufbarcodelen, 0, 4);
            int barcodelen = Convert.ToInt32(ConvertByteArrayToLong(bufbarcodelen));

            if (barcodelen > 0)
            {
                buf = new byte[barcodelen];
                temp_revbuf = new byte[barcodelen];
                received_Total = 0;
                remain_length = buf.Length;//已收文件头被覆盖
                time_count = 100;
                while (true)
                {
                    if (remain_length > 0)
                    {
                        try
                        {
                            received = netWStream.Read(temp_revbuf, 0, remain_length);
                            Buffer.BlockCopy(temp_revbuf, 0, buf, received_Total, received);
                            received_Total += received;
                            remain_length -= received;
                        }
                        catch (Exception ex)
                        {
                            LogWrite("Read barcodelen Exception " + ex.ToString());
                            xdata.error_msg = "Read barcodelen Exception " + ex.ToString();
                            return false;
                        }
                    }
                    else
                    {
                        LogWrite("received_Total == " + received_Total.ToString());
                        break;
                    }
                    Thread.Sleep(10);
                    if (time_count-- <= 0)
                    {
                        LogWrite("Read barcodelen time out ");
                        xdata.error_msg = "Read barcodelen time out ";
                        return false;
                    }
                }

                xdata.barcode = ASCIIEncoding.UTF8.GetString(buf);//解析到的条码
            }
            #endregion

            #region 取图片名称长度
            buf = new byte[4];
            temp_revbuf = new byte[4];
            received_Total = 0;
            remain_length = buf.Length;//已收文件头被覆盖
            time_count = 100;
            while (true)
            {
                if (remain_length > 0)
                {
                    try
                    {
                        received = netWStream.Read(temp_revbuf, 0, remain_length);
                        Buffer.BlockCopy(temp_revbuf, 0, buf, received_Total, received);
                        received_Total += received;
                        remain_length -= received;
                    }
                    catch (Exception ex)
                    {
                        LogWrite("Read imagenamelen Exception " + ex.ToString());
                        xdata.error_msg = "Read imagenamelen Exception " + ex.ToString();
                        return false;
                    }
                }
                else
                {
                    LogWrite("received_Total == " + received_Total.ToString());
                    break;
                }
                Thread.Sleep(10);
                if (time_count-- <= 0)
                {
                    LogWrite("Read imagenamelen time out ");
                    xdata.error_msg = "Read imagenamelen time out ";
                    return false;
                }
            }

            if (received_Total != buf.Length)
            {
                LogWrite("received_Total != 4");
                xdata.error_msg = "received_Total != 4";
                return false;
            }
            #endregion

            #region 读图片名称
            int imagenamelen = Convert.ToInt32(ConvertByteArrayToLong(buf));

            if (imagenamelen > 0)
            {
                buf = new byte[imagenamelen];
                temp_revbuf = new byte[imagenamelen];
                received_Total = 0;
                remain_length = buf.Length;//已收文件头被覆盖
                time_count = 100;
                while (true)
                {
                    if (remain_length > 0)
                    {
                        try
                        {
                            received = netWStream.Read(temp_revbuf, 0, remain_length);
                            Buffer.BlockCopy(temp_revbuf, 0, buf, received_Total, received);
                            received_Total += received;
                            remain_length -= received;
                        }
                        catch (Exception ex)
                        {
                            LogWrite("Read imagename Exception " + ex.ToString());
                            xdata.error_msg = "Read imagename Exception " + ex.ToString();
                            return false;
                        }
                    }
                    else
                    {
                        LogWrite("received_Total == " + received_Total.ToString());
                        break;
                    }
                    Thread.Sleep(10);
                    if (time_count-- <= 0)
                    {
                        LogWrite("Read imagename time out ");
                        xdata.error_msg = "Read imagename time out ";
                        return false;
                    }
                }

                xdata.imgname = ASCIIEncoding.UTF8.GetString(buf);//解析到的图片名称
            }
            #endregion

            #region 取图片长度
            buf = new byte[4];
            temp_revbuf = new byte[4];
            received_Total = 0;
            remain_length = buf.Length;//已收文件头被覆盖
            time_count = 100;
            while (true)
            {
                if (remain_length > 0)
                {
                    try
                    {
                        received = netWStream.Read(temp_revbuf, 0, remain_length);
                        Buffer.BlockCopy(temp_revbuf, 0, buf, received_Total, received);
                        received_Total += received;
                        remain_length -= received;
                    }
                    catch (Exception ex)
                    {
                        LogWrite("Read imagelen Exception " + ex.ToString());
                        xdata.error_msg = "Read imagelen Exception " + ex.ToString();
                        return false;
                    }
                }
                else
                {
                    LogWrite("received_Total == " + received_Total.ToString());
                    break;
                }
                Thread.Sleep(10);
                if (time_count-- <= 0)
                {
                    LogWrite("Read imagelen time out ");
                    xdata.error_msg = "Read imagelen time out ";
                    return false;
                }
            }

            if (received_Total != buf.Length)
            {
                LogWrite("received_Total != 4");
                xdata.error_msg = "received_Total != 4";
                return false;
            }
            #endregion

            #region 读图片数据
            long imagedatalen = Convert.ToInt32(ConvertByteArrayToLong(buf));

            if (imagedatalen > 0)
            {
                buf = new byte[imagedatalen];
                temp_revbuf = new byte[imagedatalen];
                received_Total = 0;
                remain_length = buf.Length;//已收文件头被覆盖
                time_count = 100;
                while (true)
                {
                    if (remain_length > 0)
                    {
                        try
                        {
                            received = netWStream.Read(temp_revbuf, 0, remain_length);
                            Buffer.BlockCopy(temp_revbuf, 0, buf, received_Total, received);
                            received_Total += received;
                            remain_length -= received;
                        }
                        catch (Exception ex)
                        {
                            LogWrite("Read imagedata Exception " + ex.ToString());
                            xdata.error_msg = "Read imagedata Exception " + ex.ToString();
                            return false;
                        }
                    }
                    else
                    {
                        LogWrite("received_Total == " + received_Total.ToString());
                        break;
                    }
                    Thread.Sleep(10);
                    if (time_count-- <= 0)
                    {
                        LogWrite("Read imagedata time out ");
                        xdata.error_msg = "Read imagedata time out ";
                        return false;
                    }
                }

                xdata.img = Image.FromStream(new MemoryStream(buf));//图片
            }
            #endregion

            #region 取扫描时间，校验码
            buf = new byte[19];
            temp_revbuf = new byte[19];
            received_Total = 0;
            remain_length = buf.Length;//已收文件头被覆盖
            time_count = 100;
            while (true)
            {
                if (remain_length > 0)
                {
                    try
                    {
                        received = netWStream.Read(temp_revbuf, 0, remain_length);
                        Buffer.BlockCopy(temp_revbuf, 0, buf, received_Total, received);
                        received_Total += received;
                        remain_length -= received;
                    }
                    catch (Exception ex)
                    {
                        LogWrite("Read scantime Exception " + ex.ToString());
                        xdata.error_msg = "Read scantime Exception " + ex.ToString();
                        return false;
                    }
                }
                else
                {
                    LogWrite("received_Total == " + received_Total.ToString());
                    break;
                }
                Thread.Sleep(10);
                if (time_count-- <= 0)
                {
                    LogWrite("Read scantime time out ");
                    xdata.error_msg = "Read scantime time out ";
                    return false;
                }
            }

            if (received_Total != buf.Length)
            {
                LogWrite("received_Total != 19");
                xdata.error_msg = "received_Total != 19";
                return false;
            }
            xdata.scantime = Encoding.Default.GetString(buf);
            //DateTime.TryParseExact(ASCIIEncoding.UTF8.GetString(buf), "yyyyMMddHHmmssfff", CultureInfo.CurrentCulture, DateTimeStyles.None, out xdata.scantime);
            #endregion

            return true;
        }

        private void Form1_FormClosed(object sender, FormClosedEventArgs e)
        {
            m_Measure_thread_exit = true;
        }

        private void btnStop_Click(object sender, EventArgs e)
        {
            m_Measure_thread_exit = true;
            btnStop.Enabled = false;
            btnStart.Enabled = true;
        }
        public byte[] ConvertLongToByteArray(long m)//大于Int32.MaxValue(4GB)不支持
        {
            byte[] arry = new byte[4];
            arry[0] = (byte)(m & 0xFF);
            arry[1] = (byte)((m >> 8) & 0xFF);
            arry[2] = (byte)((m >> 16) & 0xFF);
            arry[3] = (byte)((m >> 24) & 0xFF);
            return arry;
        }
        public long ConvertByteArrayToLong(byte[] arry)//大于Int32.MaxValue(4GB)不支持
        {
            long m = 0;
            m += arry[0];
            m += arry[1] * 0x100;
            m += arry[2] * 0x10000;
            m += arry[3] * (long)0x1000000;
            return m;
        }

        private void button1_Click(object sender, EventArgs e)
        {
            //清空
            lock (objviewlocker)
            {
                txtBarcode.Text = string.Empty;
                txtBarcode.Text = string.Empty;
                listRecord.Items.Clear();
                pictureboxBMP.Image = null;
            }
        }

        #region Test
        private void Test()
        {
            //byte receivedata = 0x18;//00011000

            ////00011000
            ////&
            ////00000001
            ////=
            ////00000000
            //bool ret = (receivedata & 0x01) != 0;//false

            //ret = (receivedata & 0x02) != 0;//false
            //ret = (receivedata & 0x04) != 0;//false
            //ret = (receivedata & 0x08) != 0;//true 既有I5信号
            //ret = (receivedata & 0x10) != 0;//true 又有I6信号
            //ret = false;

            //byte[] ret = ConvertLongToByteArray(65536);
            //long intvalue = ConvertByteArrayToLong(ret);

            //ret = ConvertLongToByteArray(65535);
            //intvalue = ConvertByteArrayToLong(ret);

            //ret = ConvertLongToByteArray(int.MaxValue);
            //intvalue = ConvertByteArrayToLong(ret);

            //ret = ConvertLongToByteArray(int.MaxValue - 1);
            //intvalue = ConvertByteArrayToLong(ret);

            //ret = ConvertLongToByteArray(int.MaxValue + (long)1);
            //intvalue = ConvertByteArrayToLong(ret);

            //ret = ConvertLongToByteArray(long.MaxValue);
            //intvalue = ConvertByteArrayToLong(ret);

            //ret = ConvertLongToByteArray(long.MaxValue - 1);
            //intvalue = ConvertByteArrayToLong(ret);

            //intvalue = 0;
            //ret = null;
        }
        #endregion

        #region 输出日志
        static object loglocker = new object();
        public static void LogWrite(string message)
        {
            lock (loglocker)
            {
                File.AppendAllText(Path.Combine(log_path, DateTime.Now.ToString("yyyyMMdd_HH") + ".log"), "\r\n" + DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss") + " " + message);
            }
        }
        public static void LogData(string message)
        {
            lock (loglocker)
            {
                File.AppendAllText(Path.Combine(log_path, DateTime.Now.ToString("yyyyMMdd_HH") + ".data"), "\r\n" + message);
            }
        }
        public static void LogWrite(byte[] data)
        {
            LogWrite(GetHexOutput(data, true));
        }

        /// <summary>
        /// 把指定的二进制值显示为适合记入日志的文本串
        /// </summary>
        /// <param name="data"></param>
        /// <returns></returns>
        public static string GetHexOutput(byte[] data, bool printChar)
        {
            if (data == null) return "";
            string result = "";
            int start = 0;
            while (start < data.Length)
            {
                if (!result.Equals("")) result += "\r\n";
                int len = data.Length - start;
                if (printChar)
                {
                    if (len > 16)
                        len = 16;
                }
                else
                    if (len > 8) len = 8;
                string strByte = BitConverter.ToString(data, start, len);
                strByte = strByte.Replace('-', ' ');
                if (len > 8)
                {
                    strByte = strByte.Substring(0, 23) + "-" + strByte.Substring(24);
                }
                if (printChar)
                    if (len < 16)
                    {
                        strByte += new string(' ', (16 - len) * 3);
                    }
                if (printChar)
                {
                    strByte += "  ";
                    for (int i = 0; i < len; i++)
                    {
                        char c = Convert.ToChar(data[start + i]);
                        if (char.IsControl(c))
                        {
                            c = '.';
                        }
                        strByte += c;
                    }
                }
                result += strByte;
                start += len;
            }
            return result;
        }
        #endregion

        private void Form1_Load(object sender, EventArgs e)
        {
            if (!Directory.Exists(log_path))
            {
                Directory.CreateDirectory(log_path);
            }
            if (!Directory.Exists(photo_path))
            {
                Directory.CreateDirectory(photo_path);
            }
            if (!Directory.Exists(Path.Combine(photo_path, DateTime.Now.ToString("yyyyMMdd"))))
            {
                Directory.CreateDirectory(Path.Combine(photo_path, DateTime.Now.ToString("yyyyMMdd")));
            }
        }

        private void Form1_FormClosing(object sender, FormClosingEventArgs e)
        {

        }
    }
}
