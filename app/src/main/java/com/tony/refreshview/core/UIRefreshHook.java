package com.tony.refreshview.core;

/**
 * Run a hook runnable, the runnable will run only once.
 * After the runnable is done, call resume to resume.
 * Once run, call takeover will directory call the resume action
 * 钩子任务类，实现了 Runnable 接口，可以理解为在原来的操作之间，插入了一段任务去执行。
 * 一个钩子任务只能执行一次，通过调用 takeOver 去执行。执行结束，用户需要调用 resume 方法，去恢复执行原来的操作。
 * 如果钩子任务已经执行过了，调用 takeOver 将会直接恢复执行原来的操作。
 * 可以通过 PtrFrameLayout 类的 setRefreshCompleteHook(UIRefreshHook hook) 进行设置。
 * 当用户调用 refreshComplete() 方法表示刷新结束以后，如果有 hook 存在，先执行 hook 的 takeOver 方法，执行结束，
 * 用户需要主动调用 hook 的 resume 方法，然后才会进行 Header 回弹到顶部的动作。
 */
public abstract class UIRefreshHook implements Runnable {

    private Runnable mResumeAction;
    private static final byte STATUS_PREPARE = 0;
    private static final byte STATUS_IN_HOOK = 1;
    private static final byte STATUS_RESUMED = 2;
    private byte mStatus = STATUS_PREPARE;

    public void takeOver() {
        takeOver(null);
    }

    public void takeOver(Runnable resumeAction) {
        if (resumeAction != null) {
            mResumeAction = resumeAction;
        }
        switch (mStatus) {
            case STATUS_PREPARE:
                mStatus = STATUS_IN_HOOK;
                run();
                break;
            case STATUS_IN_HOOK:
                break;
            case STATUS_RESUMED:
                resume();
                break;
        }
    }

    public void reset() {
        mStatus = STATUS_PREPARE;
    }

    public void resume() {
        if (mResumeAction != null) {
            mResumeAction.run();
        }
        mStatus = STATUS_RESUMED;
    }

    /**
     * Hook should always have a resume action, which is hooked by this hook.
     *
     * @param runnable
     */
    public void setResumeAction(Runnable runnable) {
        mResumeAction = runnable;
    }
}