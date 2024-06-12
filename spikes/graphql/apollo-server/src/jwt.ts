import jsonwebtoken from 'jsonwebtoken';

import dotenv from 'dotenv';
dotenv.config();

const { JWT_SECRET } = process.env;

export const getVerifiedToken = (token: string) => {
    try {
        if (token) {
          console.log("incoming token = " + token);
          return jsonwebtoken.verify(token, JWT_SECRET!)
        }
        return null
    } catch (error) {
        return null
    }
}