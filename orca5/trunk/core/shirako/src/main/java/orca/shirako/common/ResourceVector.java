package orca.shirako.common;



public class ResourceVector
{
    protected int[] vector;
    
    public static ResourceVector Negate(ResourceVector rv)
    {
        int[] vector = new int[rv.vector.length];
        for (int i = 0; i < vector.length; i++){
            vector[i] = - rv.vector[i];
        }
        return new ResourceVector(vector);
    }
    
    public static ResourceVector Zero(ResourceVector rv)
    {
        int[] vector = new int[rv.vector.length];
        for (int i = 0; i < vector.length; i++){
            vector[i] = 0;
        }
        return new ResourceVector(vector);        
    }
    
    /**
     * Creates a new resource vector
     * with the specified number of dimensions.
     * @param dimensions number of dimensions
     */
    public ResourceVector(int dimensions)
    {   
        if (dimensions <= 0){
            throw new IllegalArgumentException("dimensions must be positive");
        }
        vector = new int[dimensions];
        for(int i = 0; i < dimensions; i++){
            vector[i] = 0;
        }
    }
    
    public ResourceVector(int[] vector)
    {
        if (vector == null){
            throw new IllegalArgumentException("vector cannot be null");
        }
        this.vector = new int[vector.length];
        System.arraycopy(vector, 0, this.vector, 0, vector.length);
    }

    public ResourceVector(ResourceVector rv)
    {
        this(rv.vector);
    }
    
    protected void enforceCompatibility(ResourceVector other)
    {
        if (other == null){
            throw new IllegalArgumentException("other cannot be null");
        }
        if (other.vector.length != this.vector.length){
            throw new IllegalArgumentException("mismatched dimensions");
        }        
    }

    /**
     * Adds the specified vector to this instance.
     * @param other vector to add
     */
    public void add(ResourceVector other)
    {
        enforceCompatibility(other);
        for (int i = 0;  i < vector.length; i++){
            this.vector[i] += other.vector[i];
        }
    }
    
    /**
     * Subtracts the specified vector from this instance.
     * @param other vector to subtract
     */
    public void subtract(ResourceVector other)
    {
        enforceCompatibility(other);
        for (int i = 0;  i < vector.length; i++){
            this.vector[i] -= other.vector[i];
        }
    }

    /**
     * Subtracts the specified vector from this instance.
     * @param other vector to subtract
     */
    public void multiply(int times)
    {
        for (int i = 0;  i < vector.length; i++){
            this.vector[i] *= times;
        }
    }
    
    public void subtractTimes(int times, ResourceVector other)
    {
        enforceCompatibility(other);
        for (int i = 0;  i < vector.length; i++){
            this.vector[i] -= times * other.vector[i];
        }
    }
    /**
     * Subtracts the specified vector from this instance.
     * @param other vector to subtract
     */
    public void subtractMeFromAndUpdateMe(ResourceVector other)
    {
        enforceCompatibility(other);
        for (int i = 0;  i < vector.length; i++){
            this.vector[i] = other.vector[i] - this.vector[i];
        }
    }

    public boolean isPositive()
    {
        for (int i = 0;  i < vector.length; i++){
            if (this.vector[i] <= 0){
                return false;
            }
        }
        return true;
    }
    

    /**
     * Checks if this instance contains the specified vector.
     * @param other vector to check
     * @return
     */
    public boolean contains(ResourceVector other)
    {
        enforceCompatibility(other);
        for (int i = 0; i < vector.length; i++){
            if (vector[i] > other.vector[i]){
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if this instance contains the specified vector.
     * @param other vector to check
     * @return
     */
    public boolean containsOrEquals(ResourceVector other)
    {
        enforceCompatibility(other);
        for (int i = 0; i < vector.length; i++){
            if (vector[i] >= other.vector[i]){
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if this instance has a negative value for
     * one of its dimensions.
     * @return
     */
    public boolean hasNegativeDimension()
    {
        for (int i =0; i < vector.length; i++){
            if (vector[i] < 0){
                return true;
            }
        }
        return false;
    }
    
    public boolean willHaveNegativeDimension(ResourceVector other)
    {
        enforceCompatibility(other);
        for (int i =0; i < vector.length; i++){
            if (vector[i] - other.vector[i] < 0){
                return true;
            }
        }
        return false;
    }
    
    public boolean willOverflow(ResourceVector toAdd, ResourceVector limit)
    {
        enforceCompatibility(toAdd);
        enforceCompatibility(limit);
        for (int i =0; i < vector.length; i++){
            if (vector[i] + toAdd.vector[i] > limit.vector[i]){
                return true;
            }
        }
        return true;
    }

    public boolean willOverflowOnSubtract(ResourceVector toSubtract, ResourceVector limit)
    {
        enforceCompatibility(toSubtract);
        enforceCompatibility(limit);
        for (int i =0; i < vector.length; i++){
            if (vector[i] - toSubtract.vector[i] > limit.vector[i]){
                return true;
            }
        }
        return true;
    }

    
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append("(");
        sb.append(vector[0]);
        for (int i =1; i < vector.length; i++){
            sb.append(",");
            sb.append(vector[i]);
        }
        
        sb.append(")");
        return sb.toString();
    }
}
