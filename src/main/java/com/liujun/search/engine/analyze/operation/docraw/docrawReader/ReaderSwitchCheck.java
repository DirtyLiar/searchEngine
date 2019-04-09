package com.liujun.search.engine.analyze.operation.docraw.docrawReader;

import com.liujun.search.common.flow.FlowServiceContext;
import com.liujun.search.common.flow.FlowServiceInf;
import com.liujun.search.common.io.LocalIOUtils;
import com.liujun.search.engine.analyze.constant.DocrawReaderEnum;
import com.liujun.search.utilscode.io.constant.SymbolMsg;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * 进行文件切换读取的检查
 *
 * @author liujun
 * @version 0.0.1
 * @date 2019/04/08
 */
public class ReaderSwitchCheck implements FlowServiceInf {

  public static final ReaderSwitchCheck INSTANCE = new ReaderSwitchCheck();

  @Override
  public boolean runFlow(FlowServiceContext context) throws Exception {

    FileChannel streamChannel =
        context.getObject(DocrawReaderEnum.DOCRAW_PROC_INPUT_CHANNEL.getKey());
    ByteBuffer buffer = context.getObject(DocrawReaderEnum.DOCRAW_INPUT_READER_BUFFER.getKey());

    // 进行数据读取操作
    int readIndex = streamChannel.read(buffer);

    // 如果当前未读取到数据说明已经读取完成，切换到下一个文件读取
    if (readIndex <= 0) {
      FileInputStream inputStream =
          context.getObject(DocrawReaderEnum.DOCRAW_PROC_INPUT_STREAM.getKey());

      LocalIOUtils.close(streamChannel);
      LocalIOUtils.close(inputStream);

      boolean switchFlag = openNext(context);

      // 如果切换失败，则返回失败
      if (!switchFlag) {
        return false;
      } else {
        // 切换成功，进行首个buffer的读取操作
        readIndex = streamChannel.read(buffer);
      }
    }

    // 记录下当前读取的buffer的大小
    context.put(DocrawReaderEnum.DOCRAW_PROC_READ_BUFFERSIZE.getKey(), readIndex);
    return true;
  }

  /**
   * 打开下一个文件
   *
   * @param context 处理的上下文对象信息
   * @return true 切换成功 false 结束切换失败
   * @throws IOException
   */
  private boolean openNext(FlowServiceContext context) throws IOException {
    // 1,进行指定文件的读取操作
    String[] fileList = context.getObject(DocrawReaderEnum.DOCRAW_INPUT_FILE_LIST.getKey());
    // 获取文件索引
    int index = context.getObject(DocrawReaderEnum.DOCRAW_INPUT_FILE_INDEX.getKey());

    // 当索引超过文件数时，则不再继续
    if (index + 1 >= fileList.length) {
      return false;
    }

    index = index + 1;
    // 获取文件路径
    String basePath = context.getObject(DocrawReaderEnum.DOCRAW_INPUT_BASE_PATH.getKey());

    String filePath = basePath + SymbolMsg.PATH + fileList[index];

    FileInputStream input = new FileInputStream(filePath);

    context.put(DocrawReaderEnum.DOCRAW_PROC_INPUT_STREAM.getKey(), input);

    return true;
  }
}