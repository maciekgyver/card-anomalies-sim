import matplotlib.pyplot as plt
import matplotlib.animation as animation
from collections import deque
import random

deque_length = 100
update_interval = 100
data = deque([0] * deque_length, maxlen=deque_length)


def get_new_data():
    return random.randint(0, 100)


def update(frame):
    new_data = get_new_data()
    data.append(new_data)
    ax.clear()
    ax.plot(data)
    ax.set_title("Live Data Plot")
    ax.set_xlabel("Time")
    ax.set_ylabel("Value")

fig, ax = plt.subplots()
ani = animation.FuncAnimation(fig, update, interval=update_interval)
plt.show()
