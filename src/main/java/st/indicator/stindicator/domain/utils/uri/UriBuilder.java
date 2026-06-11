package st.indicator.stindicator.domain.utils.uri;

public class UriBuilder {
    public static Builder builder() {
        return new Builder();
    }
    public static class Builder {
        private StringBuilder path = new StringBuilder();
        private StringBuilder uri =  new StringBuilder();

        public Builder path(String url) {
            path.setLength(0);//StringBuilder 초기화
            path.append(url);//
            int queryStart = path.lastIndexOf("?");

            //쿼리가 포함된 경우
            //쿼리 시작 문자인 ?가 존재하면서 가장 마지막 에 위치하지 않은 경우 이미 작성된 쿼리가 존재하므로&를 붙여 반환
            if (queryStart != -1 && queryStart != path.length() - 1) {
                path.append("&");
                return this;
            }

            //? 가 포함된 경우
            if (queryStart == path.length() - 1) {
                return this;
            }
            path.append("?");
            return this;
        }

        public Builder query(String key, String value) {
            //uri가 비어있는 경우 붙여서 바로 반환
            //"" -> "key=value"
            if (uri.isEmpty()) {
                uri.append(key).append("=").append(value);
                return this;
            }
            char c = uri.charAt(uri.length() - 1);

            //시작도 아니고 끝도 아님
            //uri끝이 ?인 경우 그냥 붙이면 되고 끝이 &이 아닌 경우만 다음 쿼리를 append 하기 위해 &을 붙임
            if (c != '&' && c != '?') {
                uri.append('&');
            }

            uri.append(key).append("=").append(value);
            return this;
        }

        public Builder query(String uriString) {
            //uriString의 양 끝에 ? 또는 & 이 있는경우 혹은 이중 하나만
            if (uriString.charAt(0) == '?') {
                uriString = uriString.substring(1);
            }
            if (uriString.charAt(uriString.length() - 1) == '&') {
                uriString = uriString.substring(0, uriString.length() - 1);
            }


            //비어있는 경우
            if (uri.isEmpty()) {
                uri.append(uriString);
                return this;
            }

            //무언가 있는경우
            char c = uri.charAt(uri.length() - 1);

            //시작도 아니고 끝도 아님
            //uri가 비어있지 않은데 &으로도 ?로도 끝나지 않는다면 key=value 형태임
            //uri끝이 ?인 경우 그냥 붙이면 되고 끝이 &이 아닌 경우만 다음 쿼리를 append 하기 위해 &을 붙임
            if (c != '&' && c != '?') {
                uri.append('&');
                uri.append(uriString);
                return this;
            }
            //있는데 바로 붙일 수 있는 생태인 경우
            uri.append(uriString);
            return this;
        }

        public String build() {
            //path만 입력하고 바로 build한 경우 예외처리
            //진짜 path만 입력한 경우
            //? 가 없는 경우와 length가 0 인 경우면 -1 == -1 로 true가 반환됨
            if (path.indexOf("?") == path.length() - 1 && !path.isEmpty() && uri.isEmpty()) {
                path.setLength(path.length() - 1);
                return path.toString();
            }

            //path만 입력했는데 이미 path?uri 형태인 경우
            //이경우 path에서는 &을 붙여 반환한다.
            if (path.lastIndexOf("&") == path.length() - 1 && !path.isEmpty() && uri.isEmpty()) {
                path.setLength(path.length() - 1);
                return path.toString();
            }
            return path.append(uri).toString();
        }
    }
}


