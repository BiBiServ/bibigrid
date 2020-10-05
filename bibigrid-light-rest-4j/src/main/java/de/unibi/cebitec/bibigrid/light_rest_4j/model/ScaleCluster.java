package de.unibi.cebitec.bibigrid.light_rest_4j.model;

import java.util.Arrays;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ScaleCluster  {

    private String mode;
    private java.math.BigDecimal count;
    private java.math.BigDecimal batch;
    private String scaling;

    public ScaleCluster () {
    }

    @JsonProperty("mode")
    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    @JsonProperty("count")
    public java.math.BigDecimal getCount() {
        return count;
    }

    public void setCount(java.math.BigDecimal count) {
        this.count = count;
    }

    @JsonProperty("batch")
    public java.math.BigDecimal getBatch() {
        return batch;
    }

    public void setBatch(java.math.BigDecimal batch) {
        this.batch = batch;
    }

    @JsonProperty("scaling")
    public String getScaling() {
        return scaling;
    }

    public void setScaling(String scaling) {
        this.scaling = scaling;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ScaleCluster ScaleCluster = (ScaleCluster) o;

        return Objects.equals(mode, ScaleCluster.mode) &&
               Objects.equals(count, ScaleCluster.count) &&
               Objects.equals(batch, ScaleCluster.batch) &&
               Objects.equals(scaling, ScaleCluster.scaling);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mode, count, batch, scaling);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class ScaleCluster {\n");
        sb.append("    mode: ").append(toIndentedString(mode)).append("\n");        sb.append("    count: ").append(toIndentedString(count)).append("\n");        sb.append("    batch: ").append(toIndentedString(batch)).append("\n");        sb.append("    scaling: ").append(toIndentedString(scaling)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }
}
