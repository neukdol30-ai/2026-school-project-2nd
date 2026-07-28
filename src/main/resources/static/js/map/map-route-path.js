/**
 * 카카오 경로 응답의 다양한 좌표 구조를 지도 LatLng 배열로 변환합니다.
 */
(() => {
    "use strict";

    const namespace = window.SecondProMap = window.SecondProMap || {};

    function createLatLngFromPayload(latValue, lngValue) {
        const lat = Number(latValue);
        const lng = Number(lngValue);

        if (!isValidKoreaCoordinate(lng, lat)) {
            return null;
        }

        return new kakao.maps.LatLng(lat, lng);
    }

    function extractRoutePath(routeData, routeType, routeIndex) {
        const source = resolveRoutePathSource(routeData, routeType, routeIndex);
        const orderedPoints = extractOrderedPathPoints(source);

        if (orderedPoints.length >= 2) {
            return removeDuplicateLatLng(orderedPoints).slice(0, 3000);
        }

        const groups = [];
        collectCoordinateGroups(source, groups);

        const points = [];
        groups.forEach((group) => {
            group.forEach((point) => points.push(point));
        });

        return removeDuplicateLatLng(points).slice(0, 3000);
    }

    function resolveRoutePathSource(routeData, routeType, routeIndex) {
        if (routeType === "publictraffic" && Array.isArray(routeData?.routes)) {
            return routeData.routes[routeIndex] || routeData.routes[0] || routeData;
        }

        if (routeData?.route) {
            return routeData.route;
        }

        return routeData;
    }

    function extractOrderedPathPoints(source) {
        const points = [];
        collectOrderedPathPoints(source, points);
        return points;
    }

    function collectOrderedPathPoints(node, points) {
        if (!node) {
            return;
        }

        if (Array.isArray(node)) {
            node.forEach((item) => collectOrderedPathPoints(item, points));
            return;
        }

        if (typeof node !== "object") {
            return;
        }

        if (node.path) {
            appendPathObjectPoints(node.path, points);
        }

        if (node.geometry) {
            appendPathObjectPoints(node.geometry, points);
        }

        Object.entries(node).forEach(([key, value]) => {
            const lowerKey = key.toLowerCase();

            if (["path", "geometry"].includes(lowerKey)) {
                return;
            }

            if (["sections", "steps", "legs", "roads", "guides", "routes"].includes(lowerKey)) {
                collectOrderedPathPoints(value, points);
            }
        });
    }

    function appendPathObjectPoints(pathObject, points) {
        if (!pathObject) {
            return;
        }

        if (Array.isArray(pathObject)) {
            parseCoordinateArray(pathObject).forEach((point) => points.push(point));
            return;
        }

        if (typeof pathObject !== "object") {
            return;
        }

        ["points", "coordinates", "vertexes", "vertices", "path"].forEach((key) => {
            if (Array.isArray(pathObject[key])) {
                parseCoordinateArray(pathObject[key]).forEach((point) => points.push(point));
            }
        });
    }

    function collectCoordinateGroups(node, groups) {
        if (!node) {
            return;
        }

        if (Array.isArray(node)) {
            const parsed = parseCoordinateArray(node);
            if (parsed.length >= 2) {
                groups.push(parsed);
                return;
            }

            node.forEach((item) => collectCoordinateGroups(item, groups));
            return;
        }

        if (typeof node !== "object") {
            return;
        }

        Object.entries(node).forEach(([key, value]) => {
            const lowerKey = key.toLowerCase();

            if (Array.isArray(value) && ["coordinates", "vertexes", "vertices", "path", "points"].includes(lowerKey)) {
                const parsed = parseCoordinateArray(value);
                if (parsed.length >= 2) {
                    groups.push(parsed);
                    return;
                }
            }

            if (["sections", "steps", "legs", "roads", "guides", "routes", "geometry", "path"].includes(lowerKey)) {
                collectCoordinateGroups(value, groups);
            }
        });
    }

    function parseCoordinateArray(value) {
        if (!Array.isArray(value)) {
            return [];
        }

        if (isFlatNumberArray(value)) {
            return parseFlatCoordinatePairs(value);
        }

        if (isCoordinatePair(value)) {
            return [new kakao.maps.LatLng(Number(value[1]), Number(value[0]))];
        }

        const points = [];
        value.forEach((item) => {
            parseCoordinateArray(item).forEach((point) => points.push(point));
        });
        return points;
    }

    function parseFlatCoordinatePairs(values) {
        const points = [];

        for (let index = 0; index < values.length - 1; index += 2) {
            const lng = Number(values[index]);
            const lat = Number(values[index + 1]);

            if (isValidKoreaCoordinate(lng, lat)) {
                points.push(new kakao.maps.LatLng(lat, lng));
            }
        }

        return points;
    }

    function isFlatNumberArray(value) {
        return Array.isArray(value)
            && value.length >= 4
            && value.every((item) => Number.isFinite(Number(item)));
    }

    function isCoordinatePair(value) {
        if (!Array.isArray(value) || value.length < 2) {
            return false;
        }

        const lng = Number(value[0]);
        const lat = Number(value[1]);
        return isValidKoreaCoordinate(lng, lat);
    }

    function isValidKoreaCoordinate(lng, lat) {
        return Number.isFinite(lng)
            && Number.isFinite(lat)
            && lng >= 123
            && lng <= 132.5
            && lat >= 32
            && lat <= 39.8;
    }

    function removeDuplicateLatLng(points) {
        const result = [];
        const seen = new Set();

        points.forEach((point) => {
            const key = `${point.getLat().toFixed(6)},${point.getLng().toFixed(6)}`;
            if (!seen.has(key)) {
                seen.add(key);
                result.push(point);
            }
        });

        return result;
    }

    namespace.routePath = Object.freeze({
        createLatLngFromPayload,
        extractRoutePath
    });
})();
