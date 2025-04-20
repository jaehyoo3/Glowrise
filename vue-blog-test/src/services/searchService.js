import axios from 'axios';

const API_URL = 'http://localhost:8080/api/search';

export const searchIntegrated = async (query, page = 0, size = 10) => {
    if (!query) {

        const emptyPage = {
            content: [],
            number: 0,
            size: size,
            totalPages: 0,
            totalElements: 0,
            first: true,
            last: true
        };
        return {posts: emptyPage, users: emptyPage};
    }

    try {
        const response = await axios.get(API_URL, {
            params: {
                query,
                page,
                size,
            }
        });
        return response.data;
    } catch (error) {
        console.error("Search API 호출 중 오류 발생:", error);

        throw error;
    }
};
