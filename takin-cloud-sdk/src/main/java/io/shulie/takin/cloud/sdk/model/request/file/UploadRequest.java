package io.shulie.takin.cloud.sdk.model.request.file;

import java.io.File;
import java.util.List;

import io.shulie.takin.cloud.ext.content.trace.ContextExt;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 上传文件请求
 *
 * @author 张天赐
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UploadRequest extends ContextExt {
    /**
     * 接收方字段名称
     */
    String fieldName = "file";
    /**
     * 文件列表
     */
    List<File> fileList;
}
