package ru.sbrf.chatx.smartgate.balancer;

import org.springframework.cloud.gateway.support.NotFoundException;

public interface HealthService {

    static int getHostIndex(int[] allowedIndexHostLocal, int indexAllowed) {
        int indexResult = allowedIndexHostLocal[indexAllowed];
        if (indexResult >= 0) {
            return indexResult;
        } else {
            //хост, который высчитали, не является активным, пытаемся найти следующий активный хост
            for (int i = 1; i < allowedIndexHostLocal.length; i++) {
                int ind = indexAllowed + i;
                if (ind >= allowedIndexHostLocal.length) {
                    ind = ind - allowedIndexHostLocal.length;
                }
                //перебираем все и ищем активный
                int indRes = allowedIndexHostLocal[ind];
                if (indRes >= 0) {
                    return indRes;
                }
            }
        }
        throw NotFoundException.create(false, "Доступных сервисов не найдено");
    }

    int[] getAllowedIndexHost();

    void start() throws Exception;

    void stop() throws Exception;

    HostServiceInstance getInstance(int index);
}
