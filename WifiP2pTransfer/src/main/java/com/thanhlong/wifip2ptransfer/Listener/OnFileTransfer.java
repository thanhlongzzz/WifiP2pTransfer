package com.thanhlong.wifip2ptransfer.Listener;

public interface OnFileTransfer {
    void onCopying(Integer percent);
    void onCopied(String path);
    void onCopiedSuccess();
    void onCopyFailed(String mess);
}
