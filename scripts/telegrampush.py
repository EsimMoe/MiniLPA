import asyncio
import os
import argparse
import telegram

async def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('-token', required=True)
    parser.add_argument('-target', required=True)
    parser.add_argument('-path', required=True)
    parser.add_argument('-message', required=True)
    args = parser.parse_args()
    bot = telegram.Bot(args.token, base_url='https://tg.git.llc/bot')
    async with bot:
        file_paths = []
        for root, dirs, files in os.walk(args.path):
            for file_name in files:
                file_path = os.path.join(root, file_name)
                file_paths.append(file_path)
        async def filter_and_send(check, message = None):
            medias = list(map(lambda path: telegram.InputMediaDocument(media=open(path, 'rb')), filter(check, file_paths)))
            if medias:
                if message: medias[-1] = telegram.InputMediaDocument(media=medias[-1].media, caption=message)
                await bot.send_media_group(chat_id=args.target, media=medias, read_timeout=300, write_timeout=300)

        line = telegram.helpers.escape_markdown(text='==========================', version=2)
        await bot.send_message(chat_id=args.target, text=f'{line}\n{args.message}', parse_mode=telegram.constants.ParseMode.MARKDOWN_V2, read_timeout=300, write_timeout=300)
        await filter_and_send(lambda path: 'Windows' in path, 'Windows')
        await filter_and_send(lambda path: 'Linux' in path, 'Linux')
        await filter_and_send(lambda path: 'macOS' in path, 'macOS')
        await filter_and_send(lambda path: path.endswith('.jar'))
        await bot.send_message(chat_id=args.target, text=line, parse_mode=telegram.constants.ParseMode.MARKDOWN_V2, read_timeout=300, write_timeout=300)

if __name__ == '__main__':
    asyncio.run(main())