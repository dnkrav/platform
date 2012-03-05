package platform.server.data.expr;

import platform.base.BaseUtils;
import platform.server.caches.hash.HashContext;
import platform.server.classes.IntegralClass;
import platform.server.data.expr.query.PartitionExpr;
import platform.server.data.expr.where.cases.CaseExpr;
import platform.server.data.query.CompileSource;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.where.Where;

import java.util.*;

public class LinearOperandMap extends HashMap<Expr,Integer> {

    public IntegralClass getType() {
        assert size()>0;

        IntegralClass type = null;
        for(Expr expr : keySet())
            if(!(expr instanceof KeyExpr)) {
                IntegralClass exprType = (IntegralClass) expr.getSelfType();
                if(type==null)
                    type = exprType;
                else
                    type = (IntegralClass)type.getCompatible(exprType);
            }
        return type;        
    }

    private void add(LinearOperandMap map, int coeff) {
        for(Map.Entry<Expr, Integer> addOperand : map.entrySet())
            add(addOperand.getKey(),addOperand.getValue()*coeff);
    }

    // !!!! он меняется при add'е, но конструктора пока нету так что все равно
    void add(Expr expr,int coeff) {
        if(expr.getWhere().isFalse()) // если null не добавляем
            return;

        if(expr instanceof LinearExpr)
            add(((LinearExpr)expr).map,coeff);
        else {
            Integer prevCoeff = get(expr);
            if(prevCoeff!=null)
                coeff = coeff + prevCoeff;
            if(coeff==0)
                remove(expr);
            else
                put(expr,coeff);
        }
    }

    public int hashOuter(HashContext hashContext) {
        int result = 0;
        for(Map.Entry<Expr,Integer> operand : entrySet())
            result += (operand.getValue()-1)*31 + operand.getKey().hashOuter(hashContext);
        return result;
    }

    public String getSource(CompileSource compile) {

//        if(size()==1) {
//            Map.Entry<Expr,Integer> operand = BaseUtils.singleEntry(this);
//            return "(" + addToString(true, operand.getKey().getSource(compile), operand.getValue()) + ")";
//        }

        String source = "";
//        Where linearWhere = Where.FALSE;
//        Collection<String> orderWhere = new ArrayList<String>();
        for(Map.Entry<Expr,Integer> operand : entrySet()) {
            assert operand.getValue()!=0;
            source = source + addToString(source.length() == 0, compile.syntax.isNULL(operand.getKey().getSource(compile), "0", true), operand.getValue());
/*            if(PartitionExpr.isWhereCalculated(operand.getKey())) // ?? зачем
                orderWhere.add(operand.getKey().getSource(compile)+" IS NOT NULL");
            else
                linearWhere = linearWhere.or(operand.getKey().getWhere());*/
        }
        return "notZero(" + source + ")";//"(CASE WHEN " + linearWhere.getSource(compile) + (orderWhere.size()==0?"":" OR "+BaseUtils.toString(orderWhere," OR ")) + " THEN " + (source.length()==0?"0":source) + " ELSE " + SQLSyntax.NULL + " END)";
    }

    public String toString() {
        String result = "";
        for(Map.Entry<Expr,Integer> operand : entrySet())
            result = result + addToString(result.length() == 0, operand.getKey().toString(), operand.getValue());
        return "L(" + result + ")";
    }

    protected static String addToString(boolean first, String string, int coeff) {
        assert coeff!=0;
        if(coeff>0) return (first?"":"+") + (coeff==1?"":coeff+"*") + string;
        if(coeff==-1) return "-"+string;
        return coeff+"*"+string; // <0
    }

    public Expr packFollowFalse(Where where) {
        LinearOperandMap followedMap = new LinearOperandMap();
        for(Map.Entry<Expr,Integer> operand : entrySet()) {
            assert !operand.getValue().equals(0);
            followedMap.add(operand.getKey().followFalse(where, true),operand.getValue());
        }
        return followedMap.getExpr();
    }

    public Expr getExpr() {
        if(size()==0)
            return CaseExpr.NULL;

//        if(size()==1) {
//            Map.Entry<Expr, Integer> entry = BaseUtils.singleEntry(this);
//            if(entry.getValue().equals(1))
//                return entry.getKey();
//        }
        return new LinearExpr(this);
    }
}
