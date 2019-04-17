package com.thanhlong.wifip2ptransfer.Listener;

public interface OnFileTransfer {
    void onCopying(Integer percent);
    void onCopied();
    void onCopyFailed(String mess);
}
