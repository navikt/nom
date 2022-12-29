package no.nav.nom.libs.lucenewrapper.search;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class QueryFilterContext {

    private final Set<QueryFilter<?>> filters = new HashSet<>();

    private boolean defaultInclude = true;

    public QueryFilterContext(){
    }

    public QueryFilterContext(QueryFilter<?> filter){
        this.filters.add(filter);
    }

    public QueryFilterContext(Collection<QueryFilter<?>> filters) {
        this.filters.addAll(filters);
    }

    public Set<QueryFilter<?>> all(){
        return this.filters;
    }

    public void setDefaultInclude(boolean include){
        this.defaultInclude = include;
    }

    public boolean add(QueryFilter<?> filter){
        return this.filters.add(filter);
    }

    public QueryFilter<?> getByEntityClass(Class<?> clazz){
        var preCast = filters.stream().filter(it -> it.getEntityClass().equals(clazz)).findFirst().orElse(null);
        var specific = (QueryFilter<?>) preCast;

        return Objects.requireNonNullElseGet(specific, () -> new QueryFilter() {
            @Override
            public Class<?> getEntityClass() {
                return clazz;
            }

            @Override
            public Mode mode() {
                return defaultInclude ? Mode.INCLUDE : Mode.EXCLUDE;
            }
        });
    }


    public static QueryFilterContext of(Collection<QueryFilter<?>> filters){
        return new QueryFilterContext(filters);
    }

    public static QueryFilterContext byClasses(Collection<Class<?>> includedClasses){
        var out = new QueryFilterContext();
        out.setDefaultInclude(false);
        for( var clazz : includedClasses){
            out.add(new QueryFilter() {

                @Override
                public Class<?> getEntityClass() {
                    return clazz;
                }

                @Override
                public Mode mode() {
                    return Mode.INCLUDE;
                }
            });
        }
        return out;
    }


}
