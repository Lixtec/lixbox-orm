package fr.lixbox.orm.redis.model;

import redis.clients.jedis.search.Query;

/**
 * Query represents query parameters and filters to load results from the engine
 */
public class EQuery extends Query
{
    private String _queryString = "*";
    
    
    public EQuery() 
    {
        super("*");
        this._queryString="*";
        this.limit(0, 500);
    }



    public EQuery(String queryString) 
    {
        super(queryString);
        this._queryString=queryString;
        this.limit(0, 500);
    }
    
    
    
    @Override
    public String toString()
    {
        return "Query [ " + this._queryString + "]";
    }
}
