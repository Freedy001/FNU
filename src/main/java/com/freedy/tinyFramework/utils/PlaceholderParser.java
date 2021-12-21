package com.freedy.tinyFramework.utils;

import java.util.*;

/**
 * <h2>普通替换 (?)</h2>
 * 替换待解析语句中的 <b>?</b> 为你指定的参数 <br/>
 * 例如new SqlParamParser("select * from table where id = ? and name = ?", 12 , "xiao") <br/>
 * toString()方法就输出 select * from table where id = 12 and name = 'xiao' <br/>
 * <br/>
 * <h2>连锁参数 (?*)</h2>
 * 例如当待解析语句中出现 <b>?*</b> ,恰巧你也传入了一个集合,那么该集合就会被用逗号来展开.
 * <pre>
 * 例如：
 *   {@code
 *      SqlParamParser str = new SqlParamParser("select * from user where name ='jack' and id in (?*)", 12 , Arrays.asList("xiao","data"));
 *      System.out.println(str);
 *   }
 *   则控制台输出 select * from table where name ='jack' id in ('xiao','data')
 * </pre>
 * 如果对应的参数不是集合则默认取参数数组的后续所有元素在此展开，你也可以通过configSerialParams()方法来指定连锁参数的个数。
 * <pre>
 * 例如：
 *   {@code
 *    SqlParamParser str = new SqlParamParser(
 *          "select * from user where name like ? and id in (?*) and age=?",
 *          "jack", 43, 12, 54, 65, 20).configSerialParams(",", 4)
 *          );
 *    System.out.println(str);
 *   }
 * </pre>
 * <br/>
 * <h3>禁用HIGHLIGHT，则可以在字符串前面加上 {!} </h3>
 * <pre>
 * 例如：
 *   {@code
 *    SqlParamParser str = new SqlParamParser("select * from ?" , "{!}user");
 *    System.out.println(str);
 *   }
 * 则控制台输出 select * from user
 * </pre>
 *
 * @author Freedy
 * @date 2021/9/22 16:37
 */
public class PlaceholderParser {

    private final StringBuilder sqlBuilder = new StringBuilder();
    private final ArrayList<Object> params = new ArrayList<>();
    private String toStringCache;
    private String split = ",";
    private int[] eachSerialParamsSize = new int[0];
    private Object emptyCharacter;
    private final Map<Integer, String> indexSplit = new HashMap<>();
    private int serialParamsIndex = 0;
    private String highLightStrStart;
    private String highLightStrEnd;
    private String highLightNumStart;
    private String highLightNumEnd;
    private final List<Class<?>> noneHighLightClass=new ArrayList<>();
    /**
     * 解析sql
     *
     * @param str    如果sql中需要<Bold>?<Bold/>不被解析为占位符,则可以使用转义字符<Bold>&<Bold/> <br/>
     *               例如:<Bold>?<Bold/>可以用<Bold>&?<Bold/>来代替,<Bold>&<Bold/>可以用<Bold>&&<Bold/>来代替  <br/>
     * @param params 替换'?'的参数
     */
    public PlaceholderParser(String str, Object... params) {
        this.sqlBuilder.append(str);
        this.params.addAll(Arrays.asList(params));
        configPlaceholderHighLight(PlaceholderHighLight.NONE);
    }

    public PlaceholderParser reset(String str, Object... params) {
        sqlBuilder.delete(0, sqlBuilder.length());
        toStringCache = null;
        this.sqlBuilder.append(str);
        this.params.clear();
        this.params.addAll(Arrays.asList(params));
        return this;
    }

    public PlaceholderParser clearConfig() {
        toStringCache = null;
        toStringCache = "";
        split = ",";
        eachSerialParamsSize = new int[0];
        emptyCharacter = null;
        indexSplit.clear();
        serialParamsIndex = 0;
        configPlaceholderHighLight(PlaceholderHighLight.NONE);
        return this;
    }

    /**
     * 指定连锁参数的分割方式与每个连环参数分别占你导入参数的个数，用法详情见类上面的javadoc
     *
     * @param eachSerialParamsSize 每个连环参数的大小
     */
    public PlaceholderParser configSerialParams(int... eachSerialParamsSize) {
        toStringCache = null;
        if (eachSerialParamsSize.length > 0) {
            for (int i : eachSerialParamsSize) {
                if (i <= 1) throw new IllegalArgumentException("each size must great than 1");
            }
            this.eachSerialParamsSize = eachSerialParamsSize;
        }
        return this;
    }

    /**
     * 指定连环参数默认分隔符，如果不指定则默认为 ","
     *
     * @param split 默认分隔符
     */
    public PlaceholderParser serialParamsSplit(String split) {
        toStringCache = null;
        this.split = split;
        return this;
    }

    /**
     * 指定位置的分隔符
     */
    public PlaceholderParser serialParamsSplit(int index, String split) {
        toStringCache = null;
        indexSplit.put(index, split);
        return this;
    }

    /**
     * 当你输入的参数与 <b>?</b> 的个数不匹配时将使用你指定的字符填充.
     */
    public PlaceholderParser ifNullFillWith(Object nullCharacter) {
        toStringCache = null;
        int questionSymbolNum = 0;
        for (char c : sqlBuilder.toString().toCharArray()) {
            if (c == '?') questionSymbolNum++;
        }

        for (int i = params.size(); i < questionSymbolNum; i++) {
            params.add(nullCharacter);
        }
        return this;
    }

    /**
     * 仅当当输入的sql有连环参数时启用 <br/>
     * 如果传入的是集合且集合为空则用你指定的字符填充
     */
    public PlaceholderParser ifEmptyFillWith(Object emptyCharacter) {
        toStringCache = null;
        this.emptyCharacter = emptyCharacter;
        return this;
    }

    /**
     * 根据条件来拼接sql,第2,3个参数同构造方法两个参数
     */
    public PlaceholderParser join(boolean condition, String joinSql, Object... params) {
        if (condition) {
            toStringCache = null;
            sqlBuilder.append(joinSql);
            this.params.addAll(Arrays.asList(params));
        }
        return this;
    }

    public PlaceholderParser registerNoneHighLightClass(Class<?> ...classes){
        toStringCache = null;
        noneHighLightClass.addAll(Arrays.asList(classes));
        return this;
    }

    public PlaceholderParser configPlaceholderHighLight(PlaceholderHighLight highLight) {
        toStringCache=null;
        switch (highLight) {
            case NONE -> {
                setStart("");
                setEnd("");
            }
            case SQL_STYLE -> {
                highLightStrStart = "'";
                highLightStrEnd = "'";
                highLightNumStart = "";
                highLightNumEnd = "";
            }
            case HIGH_LIGHT_RED -> {
                setStart("\033[31m");
                setEnd("\033[0;39m");
            }
            case HIGH_LIGHT_BLUE -> {
                setStart("\033[34m");
                setEnd("\033[0;39m");
            }
            case HIGH_LIGHT_CYAN -> {
                setStart("\033[36m");
                setEnd("\033[0;39m");
            }
            case HIGH_LIGHT_GREEN -> {
                setStart("\033[32m");
                setEnd("\033[0;39m");
            }
            case HIGH_LIGHT_YELLOW -> {
                setStart("\033[93m");
                setEnd("\033[0;39m");
            }
        }
        return this;
    }

    private void setStart(String start) {
        toStringCache=null;
        highLightNumStart = start;
        highLightStrStart = start;
    }

    private void setEnd(String end) {
        toStringCache=null;
        highLightNumEnd = end;
        highLightStrEnd = end;
    }


    @Override
    public String toString() {
        if (StringUtils.hasText(toStringCache)) return toStringCache;
        StringBuilder sqlString = new StringBuilder();
        String sql = this.sqlBuilder.toString();
        Object[] params = this.params.toArray();
        int[] eachSerialParamsSize = this.eachSerialParamsSize;
        int eachSerialParamsSizeIndex = 0;

        int length = sql.length();
        int lastSplitIndex = 0;
        for (int i = 0, index = 0; i < length; i++) {
            //检测转义字符
            if (sql.charAt(i) == '&') {
                char c = 0;
                try {
                    c = sql.charAt(i + 1);
                } catch (StringIndexOutOfBoundsException ignore) {
                }
                if (c == '?') {
                    sql = sql.substring(0, i) + "?" + sql.substring(i + 2);
                    length--;
                    continue;
                } else if (c == '&') {
                    sql = sql.substring(0, i) + "&" + sql.substring(i + 2);
                    length--;
                    continue;
                } else {
                    throw new IllegalArgumentException("invalid escape character &" + c + " in position " + i + "," + (i + 1));
                }
            }
            if (sql.charAt(i) == '?') {
                Object param;
                try {
                    if (i + 1 < length && sql.charAt(i + 1) == '*') {
                        //解析连锁参数
                        param = params[index];
                        if (param instanceof Object[]) {
                            param = Arrays.asList((Object[]) param);
                        }
                        if (param instanceof Collection<?> dataList) {
                            //对应的参数是数组
                            int size = dataList.size();
                            if (size > 0) {
                                sqlString.append(sql, lastSplitIndex, i);
                                sqlString.append(foldDataArray(dataList.toArray(), 0, dataList.size()));
                                index++;
                            } else {
                                if (emptyCharacter != null) {
                                    sqlString.append(sql, lastSplitIndex, i);
                                    appendByDataType(sqlString, "", emptyCharacter);
                                    index++;
                                } else
                                    throw new IllegalArgumentException("The collection corresponding to '?*' is empty");
                            }
                        } else {
                            //对应的参数非数组
                            sqlString.append(sql, lastSplitIndex, i);
                            if (eachSerialParamsSize.length > 0) {
                                sqlString.append(foldDataArray(params, index, eachSerialParamsSize[eachSerialParamsSizeIndex]));
                                index += eachSerialParamsSize[eachSerialParamsSizeIndex];
                                eachSerialParamsSizeIndex++;
                            } else {
                                sqlString.append(foldDataArray(params, index, params.length - index));
                                index = params.length;
                            }

                        }

                        this.serialParamsIndex++;
                        lastSplitIndex = i + 2;
                        i++;
                        continue;
                    } else param = params[index++];
                } catch (NullPointerException e) {
                    throw new NullPointerException("params shouldn't be null");
                } catch (ArrayIndexOutOfBoundsException e) {
                    throw new IllegalArgumentException("your params count is not match the '?' or '?*' count in your sql parameter");
                }
                sqlString.append(sql, lastSplitIndex, i);
                appendByDataType(sqlString, "", param);
                lastSplitIndex = i + 1;
            }
        }
        sqlString.append(sql.substring(lastSplitIndex));
        toStringCache = sqlString.toString();
        return toStringCache;
    }

    private String foldDataArray(Object[] dataArray, int pos, int size) {
        String s = indexSplit.get(serialParamsIndex);
        String split = s == null ? this.split : s;

        StringBuilder builder = new StringBuilder();
        for (int j = 0; j < size; j++) {
            appendByDataType(builder, split, dataArray[pos++]);
        }
        if (size > 0) {
            int builderLen = builder.length();
            builder.delete(builderLen - split.length(), builderLen);
        }
        return builder.toString();
    }


    private void appendByDataType(StringBuilder sqlString, String split, Object item) {
        if (item instanceof Number) {
            sqlString.append(highLightNumStart).append(item).append(highLightNumEnd).append(split);
        } else {
            String str = String.valueOf(item);

            if ((str).startsWith("{!}")) {
                sqlString.append(str.substring(3)).append(split);
            } else {
                for (Class<?> noneHighLightClass : noneHighLightClass) {
                    if (noneHighLightClass.isInstance(item)) {
                        sqlString.append(str).append(split);
                        return;
                    }
                }
                sqlString.append(highLightStrStart).append(str).append(highLightStrEnd).append(split);
            }
        }
    }

    public enum PlaceholderHighLight {
        NONE,
        SQL_STYLE,
        HIGH_LIGHT_RED,
        HIGH_LIGHT_BLUE,
        HIGH_LIGHT_CYAN,
        HIGH_LIGHT_GREEN,
        HIGH_LIGHT_YELLOW,
    }
}
