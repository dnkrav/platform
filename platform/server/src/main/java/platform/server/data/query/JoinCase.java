package platform.server.data.query;

import platform.server.data.query.exprs.cases.Case;
import platform.server.where.Where;
             
public class JoinCase<U> extends Case<Join<U>> {

    // дублируем чтобы различать
    public JoinCase(Where iWhere, Join<U> iJoin) {
        super(iWhere,iJoin);
    }

}
