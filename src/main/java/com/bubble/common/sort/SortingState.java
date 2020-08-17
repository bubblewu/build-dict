package com.bubble.common.sort;

import java.io.IOException;

/**
 * Sort的不同阶段
 *
 * @author wugang
 * date: 2020-08-14 10:08
 **/
public interface SortingState {

    public enum Phase {
        /**
         * Sort的不同阶段
         */
        PRE_SORTING,
        SORTING,
        COMPLETE
    }

    Phase getPhase();

    /**
     * 访问器，用于确定排序器是否处于其在内存中的预排序阶段。
     *
     * @return boolean
     */
    boolean isPreSorting();

    /**
     * 访问器，用于确定排序器是否处于规则合并排序阶段。
     *
     * @return boolean
     */
    boolean isSorting();

    /**
     * 访问器，用于确定排序是否已成功完成。
     *
     * @return boolean
     */
    boolean isCompleted();

    /**
     * 访问器，用于检查在预排序阶段创建了多少预排序文件。
     * 可以是零，如果整个数据适合在内存预先排序。
     *
     * @return int
     */
    int getNumberOfPreSortFiles();

    /**
     * 对于预排序，它基本上意味着正在内存中处理的段的数量(基于0的)，
     * 对于常规排序，它是(基于0的)排序的数量。
     *
     * @return int
     */
    int getSortRound();

    /**
     * 访问器，用于计算完成排序需要执行多少常规排序轮(如果已知的话)。
     * 如果信息未知，将返回-1。这些信息通常在预排序后才可用。
     *
     * @return int
     */
    int getNumberOfSortRounds();


    /**
     * 方法，可用于尝试取消正在执行的排序操作。不会抛出任何异常;
     * 当排序线程收到该请求时，排序将被停止。
     */
    void cancel();

    /**
     * 方法，可用于尝试取消正在执行的排序操作。可以指定异常对象;
     * 如果给出了非空实例，它将被抛出以指示错误结果，否则排序将被中断，但执行将正常返回。
     *
     * @param e RuntimeException
     */
    void cancel(RuntimeException e);


    /**
     * 方法，可用于尝试取消正在执行的排序操作。可以指定异常对象;
     * 如果给出了非空实例，它将被抛出以指示错误结果，否则排序将被中断，但执行将正常返回。
     *
     * @param e IOException
     */
    void cancel(IOException e);

}
